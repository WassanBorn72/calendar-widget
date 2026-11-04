package com.example.daycalendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Private activity target for the widget's explicit PendingIntent template. */
public class OpenCalendarEventActivity extends Activity {
    private final ExecutorService queries=Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(!CalendarEvents.permitted(this)) {
            Toast.makeText(this,"カレンダーの読み取りを許可してください",Toast.LENGTH_LONG).show();
            startActivity(new Intent(this,MainActivity.class));finish();return;
        }
        Intent click=getIntent();
        if(click.hasExtra(WidgetEventAction.ID)) {
            long id=click.getLongExtra(WidgetEventAction.ID,-1),begin=click.getLongExtra(WidgetEventAction.BEGIN,0),end=click.getLongExtra(WidgetEventAction.END,-1);
            if(id<=0 || !click.hasExtra(WidgetEventAction.BEGIN) || !click.hasExtra(WidgetEventAction.END) || end<begin){finish();return;}
            open(id,begin,end,click.getBooleanExtra(WidgetEventAction.ALL_DAY,false));return;
        }
        String date=click.getStringExtra(WidgetEventAction.DATE);
        int from=click.getIntExtra(WidgetEventAction.FROM,-1),to=click.getIntExtra(WidgetEventAction.TO,-1);
        if(date==null || from<0 || to>1440 || to<=from){finish();return;}
        LocalDate day;
        try {day=LocalDate.parse(date);}catch(java.time.format.DateTimeParseException invalid){finish();return;}
        TextView loading=new TextView(this);loading.setText("予定を確認しています…");loading.setPadding(dp(24),dp(24),dp(24),dp(24));setContentView(loading);
        query(() -> {
            try {
                List<CalendarEvent> events=WidgetEventAction.inRange(DayTimeline.layout(loadEvents(day),day,ZoneId.systemDefault()),from,to);
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())choose(events);});
            } catch(RuntimeException error){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){Toast.makeText(this,"予定を読み込めません。ウィジェットを更新してください",Toast.LENGTH_LONG).show();finish();}});}
        });
    }
    protected void query(Runnable task){queries.execute(task);}
    protected List<CalendarEvent> loadEvents(LocalDate day){return CalendarEvents.load(this,day);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void choose(List<CalendarEvent> events) {
        if(events.isEmpty()){Toast.makeText(this,"予定が変更されています。ウィジェットを更新してください",Toast.LENGTH_LONG).show();finish();return;}
        if(events.size()==1){CalendarEvent e=events.get(0);open(e.id(),e.begin(),e.end(),e.allDay());return;}
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(16),dp(16),dp(16));
        TextView title=new TextView(this);title.setText("開く予定を選択");title.setTextSize(20);content.addView(title);
        List<String> labels=new ArrayList<>();for(CalendarEvent e:events)labels.add(e.time()+"\n"+e.title());
        ListView list=new ListView(this);list.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels));
        list.setOnItemClickListener((parent,view,position,id)->{CalendarEvent e=events.get(position);open(e.id(),e.begin(),e.end(),e.allDay());});
        int listHeight=Math.min(dp(360),Math.min(events.size()*dp(64),getResources().getDisplayMetrics().heightPixels/2));
        content.addView(list,new LinearLayout.LayoutParams(-1,listHeight));
        Button cancel=new Button(this);cancel.setText("キャンセル");cancel.setOnClickListener(v->finish());content.addView(cancel);
        setContentView(content);
    }
    private void open(long id,long begin,long end,boolean allDay) {
        if(!CalendarEventLauncher.open(this,id,begin,end,allDay))Toast.makeText(this,"予定を開けるカレンダーアプリが見つかりません",Toast.LENGTH_LONG).show();
        finish();
    }
    @Override protected void onDestroy(){queries.shutdownNow();super.onDestroy();}
}
