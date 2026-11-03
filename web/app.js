'use strict';
const $ = id => document.getElementById(id);
const localISO = date => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
let selected = localISO(new Date());
let widget = false;
const samples = [
  {start:'終日',end:'',title:'新しい一週間を、気持ちよく',category:'自分の時間',type:'purple',location:'小さな目標をひとつ'},
  {start:'09:30',end:'10:00',title:'チームの朝会',category:'仕事',type:'green',location:'オンライン · Google Meet'},
  {start:'11:00',end:'12:00',title:'デザインレビュー',category:'仕事',type:'green',location:'ミーティングルーム A'},
  {start:'12:30',end:'13:30',title:'友人とランチ',category:'プライベート',type:'orange',location:'駅前のカフェ'},
  {start:'15:00',end:'16:30',title:'集中する時間',category:'自分の時間',type:'purple',location:'通知をオフにして、ひとつずつ'},
  {start:'18:30',end:'19:30',title:'帰り道に、本屋へ',category:'プライベート',type:'orange',location:'いつもの書店'}
];
function render(){
  const day = new Date(`${selected}T12:00:00`);
  $('date-input').value = selected;
  const isToday = selected === localISO(new Date());
  $('relative-day').textContent = isToday ? 'TODAY / 今日' : 'YOUR DAILY AGENDA';
  $('date-title').textContent = new Intl.DateTimeFormat('ja-JP',{month:'long',day:'numeric'}).format(day);
  $('date-subtitle').textContent = new Intl.DateTimeFormat('ja-JP',{year:'numeric',weekday:'long'}).format(day);
  // Sunday intentionally provides an empty-state preview.
  const events = day.getDay() === 0 ? [] : day.getDay() === 6 ? samples.filter(e=>e.type !== 'green') : samples;
  $('summary').innerHTML = `<span><strong>${events.length}</strong> 件の予定</span><span class="divider"></span><span>${events.length ? '自分のペースで、いい一日を。' : '今日は、余白を楽しもう。'}</span>`;
  $('event-list').replaceChildren();
  const allDay = document.createElement('div'); allDay.className='all-day-events';
  events.filter(e=>!e.end).forEach(event=>allDay.append(makeCard(event)));
  if(allDay.childElementCount) $('event-list').append(allDay);
  const timeline=document.createElement('div');timeline.className='timeline';
  for(let hour=0;hour<=24;hour++){
    const line=document.createElement('div');line.className=hour===24?'hour-line day-end':'hour-line';line.style.top=`${hour*72}px`;
    const label=document.createElement('span');label.textContent=`${String(hour).padStart(2,'0')}:00`;
    line.append(label);timeline.append(line);
  }
  const timed=events.filter(e=>e.end).map(e=>({...e,begin:minutes(e.start),finish:minutes(e.end)}));
  const placed=placeEvents(timed);
  placed.forEach(event=>{
    const card=makeCard(event);card.classList.add('timed-event');
    card.style.top=`${event.begin*1.2}px`;card.style.height=`${(event.finish-event.begin)*1.2}px`;
    card.style.left=`calc(46px + (100% - 50px) * ${event.lane/event.columns})`;
    card.style.width=`calc((100% - 50px) / ${event.columns} - 3px)`;
    timeline.append(card);
  });
  const marker=document.createElement('div');marker.id='now-line';marker.className='now-line';
  marker.append(document.createElement('span'));timeline.append(marker);
  $('event-list').append(timeline);
  if(!events.length){const empty=document.createElement('p');empty.className='empty-state';empty.textContent='今日は予定のない日。時間の余白を楽しみましょう。';$('event-list').prepend(empty);}
  updateNow();
  requestAnimationFrame(()=>{
    const now=new Date();
    const hour=isToday?Math.max(0,now.getHours()-1):8;
    $('event-list').scrollTop=hour*72+(allDay.childElementCount?allDay.offsetHeight:0);
  });
  $('device').classList.toggle('widget',widget);
  $('agenda-mode').classList.toggle('selected',!widget);$('widget-mode').classList.toggle('selected',widget);
  $('agenda-mode').setAttribute('aria-pressed',String(!widget));$('widget-mode').setAttribute('aria-pressed',String(widget));
  $('preview-kind').textContent=widget?'HOME WIDGET PREVIEW':'APP PREVIEW';
  $('caption').textContent=widget?'ホーム画面に、今日の予定を。':'0〜24時を、一定の時間幅で。';
}
const minutes=value=>{const [h,m]=value.split(':').map(Number);return h*60+m;};
function placeEvents(events){
  const sorted=[...events].sort((a,b)=>a.begin-b.begin||b.finish-a.finish);
  let group=[],end=-1;
  const result=[];
  function flush(){
    const lanes=[];
    for(const e of group){let lane=lanes.findIndex(stop=>stop<=e.begin);if(lane<0)lane=lanes.length;lanes[lane]=e.finish;e.lane=lane;}
    group.forEach(e=>{e.columns=lanes.length;result.push(e);});group=[];
  }
  for(const event of sorted){if(event.begin>=end){flush();end=-1;}group.push(event);end=Math.max(end,event.finish);}
  flush();return result;
}
function makeCard(event){
  const card=document.createElement('button');card.className=`event-card ${event.type}`;
  card.setAttribute('aria-label',`${event.start}${event.end?'〜'+event.end:' 終日'} ${event.title}`);
  const title=document.createElement('h3');title.textContent=event.title;
  const duration=document.createElement('span');duration.className='event-time';duration.textContent=event.end?`${event.start} — ${event.end}`:'終日';
  const location=document.createElement('p');location.textContent=`⌖ ${event.location}`;
  card.append(duration,title,location);card.addEventListener('click',()=>openEvent(event));return card;
}
function updateNow(){
  const now=new Date(),line=$('now-line');
  const clock=now.toLocaleTimeString('ja-JP',{hour:'2-digit',minute:'2-digit'});
  $('clock').textContent=clock;$('live-clock').textContent=clock;
  if(!line)return;
  const today=selected===localISO(now);line.hidden=!today;
  if(today){line.style.top=`${(now.getHours()*60+now.getMinutes())*1.2}px`;
    line.firstChild.textContent=now.toLocaleTimeString('ja-JP',{hour:'2-digit',minute:'2-digit'});}
}
function move(days){const date=new Date(`${selected}T12:00:00`);date.setDate(date.getDate()+days);selected=localISO(date);render();}
function openEvent(event){$('dialog-title').textContent=event.title;$('dialog-time').textContent=`${selected} · ${event.start}${event.end?' — '+event.end:''}`;$('dialog-location').textContent=event.location;$('event-dialog').showModal();}
$('previous').onclick=()=>move(-1);$('next').onclick=()=>move(1);
$('today').onclick=()=>{selected=localISO(new Date());render();};
$('date-input').addEventListener('change',event=>{if(event.target.value && event.target.validity.valid){selected=event.target.value;render();}});
$('agenda-mode').onclick=()=>{widget=false;render();};
$('widget-mode').onclick=()=>{widget=true;selected=localISO(new Date());render();};
$('refresh').onclick=()=>{render();$('footer-note').textContent='プレビューを更新しました';};
$('close-dialog').onclick=()=>$('event-dialog').close();
$('event-dialog').addEventListener('click',event=>{if(event.target===$('event-dialog')){const r=event.target.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)event.target.close();}});
render();

let browserDay=localISO(new Date());
function minuteTick(){
  const day=localISO(new Date());
  if(day!==browserDay){browserDay=day;render();}else updateNow();
}
function scheduleMinute(){setTimeout(()=>{minuteTick();scheduleMinute();},60000-Date.now()%60000);}
scheduleMinute();
document.addEventListener('visibilitychange',()=>{if(!document.hidden)minuteTick();});
