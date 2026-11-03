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
  if(!events.length){const empty=document.createElement('p');empty.className='empty-state';empty.textContent='今日は予定のない日。\nひと息つきましょう。';$('event-list').append(empty);}
  events.forEach(event=>{
    const row=document.createElement('div');row.className='event-row';
    const time=document.createElement('span');time.className='time-label';time.textContent=event.start;
    const card=document.createElement('button');card.className=`event-card ${event.type}`;
    const category=document.createElement('span');category.className='category';category.textContent=event.category;
    const title=document.createElement('h3');title.textContent=event.title;
    const duration=document.createElement('span');duration.className='event-time';duration.textContent=event.end ? `${event.start} — ${event.end}` : 'ALL DAY';
    const location=document.createElement('p');location.textContent=`⌖ ${event.location}`;
    card.append(category,title,duration,location);card.addEventListener('click',()=>openEvent(event));
    row.append(time,card);$('event-list').append(row);
  });
  $('device').classList.toggle('widget',widget);
  $('agenda-mode').classList.toggle('selected',!widget);$('widget-mode').classList.toggle('selected',widget);
  $('agenda-mode').setAttribute('aria-pressed',String(!widget));$('widget-mode').setAttribute('aria-pressed',String(widget));
  $('preview-kind').textContent=widget?'HOME WIDGET PREVIEW':'APP PREVIEW';
  $('caption').textContent=widget?'ホーム画面に、今日の予定を。':'一日の予定を、縦にすっきり。';
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
