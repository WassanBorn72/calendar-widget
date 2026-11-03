"""Run with Python + Playwright and a local Chromium installation."""
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from threading import Thread
import os
from playwright.sync_api import sync_playwright

root = Path(__file__).resolve().parents[1]
output = Path(os.environ.get('SCREENSHOT_DIR', '/tmp/calendar-preview'))
output.mkdir(parents=True, exist_ok=True)
server = ThreadingHTTPServer(('127.0.0.1', 0), partial(SimpleHTTPRequestHandler, directory=str(root / 'web')))
Thread(target=server.serve_forever, daemon=True).start()
try:
    with sync_playwright() as p:
        browser = p.chromium.launch(executable_path='/usr/bin/chromium', args=['--no-sandbox'])
        page = browser.new_page(viewport={'width':1440,'height':1100}, timezone_id='Asia/Tokyo')
        errors = []
        page.on('pageerror', lambda e: errors.append(str(e)))
        page.clock.install(time=__import__('datetime').datetime.fromisoformat('2026-11-03T03:15:00+00:00'))
        page.goto(f'http://127.0.0.1:{server.server_port}')
        assert page.locator('.hour-line').count() == 25
        assert page.locator('.hour-line span').first.inner_text() == '00:00'
        assert page.locator('.hour-line span').last.inner_text() == '24:00'
        offsets = page.locator('.hour-line').evaluate_all('(els)=>els.map(e=>parseFloat(e.style.top))')
        assert all(b-a == 72 for a,b in zip(offsets,offsets[1:]))
        cards = page.locator('.timed-event')
        heights = cards.evaluate_all('(els)=>els.map(e=>parseFloat(e.style.height))')
        assert heights[0] == 36 and heights[3] == 108, heights
        assert page.locator('#now-line').is_visible()
        assert page.locator('#live-clock').inner_text() == '12:15'
        before = page.locator('#now-line').evaluate('(e)=>parseFloat(e.style.top)')
        assert abs(before - 735*1.2) < 1
        page.clock.run_for(60000)
        after = page.locator('#now-line').evaluate('(e)=>parseFloat(e.style.top)')
        assert page.locator('#live-clock').inner_text() == '12:16'
        assert abs(after-before-1.2) < 0.1
        assert page.evaluate('placeEvents([{begin:60,finish:120},{begin:90,finish:150}]).every(e=>e.columns===2)')
        cards.first.click()
        assert page.locator('dialog').is_visible()
        page.keyboard.press('Escape')
        page.locator('#next').click()
        assert page.locator('#now-line').is_hidden()
        page.locator('#date-input').fill('2026-11-08')
        page.locator('#date-input').dispatch_event('change')
        assert page.locator('.hour-line').count() == 25
        assert page.locator('.timed-event').count() == 0
        page.locator('#widget-mode').click()
        assert page.locator('#now-line').is_visible()
        assert page.locator('.hour-line').nth(1).evaluate('(e)=>parseFloat(e.style.top)') == 72
        page.locator('#agenda-mode').click()
        page.locator('#event-list').evaluate('(e)=>e.scrollTop=660')
        page.screenshot(path=str(output / 'timeline-desktop.png'), full_page=True)
        page.set_viewport_size({'width':390,'height':844})
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
        page.locator('#widget-mode').click()
        page.locator('#event-list').evaluate('(e)=>e.scrollTop=660')
        page.screenshot(path=str(output / 'timeline-mobile.png'), full_page=True)
        assert not errors, errors
        browser.close()
        print('PASS: 25 hour labels, constant scale, proportional durations, live clock, overlap lanes, event details, other-day/empty-day, widget, mobile')
finally:
    server.shutdown()
