"""Drive the actual Main process with role logins, commands and a restart (stdlib only)."""
import os, queue, subprocess, sys, tempfile, threading, time
from pathlib import Path
root=Path(__file__).resolve().parents[1]
classpath=str(root/'build')
data=tempfile.mkdtemp(prefix='rkm-console-')
transcript=[]
class App:
    def __init__(self):
        self.p=subprocess.Popen(['java','-Drkm.millisPerMinute=10','-Drkm.dataDir='+data,'-cp',classpath,'restuarant.Main'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=0)
        self.q=queue.Queue();self.pending=''
        def read():
            while True:
                ch=self.p.stdout.read(1)
                if not ch: break
                self.q.put(ch)
        threading.Thread(target=read,daemon=True).start()
    def expect(self,text):
        until=time.monotonic()+15
        while text not in self.pending:
            if time.monotonic()>until:raise AssertionError(f'Timeout waiting for {text!r}; got {self.pending!r}')
            try:
                c=self.q.get(timeout=.1);self.pending+=c;transcript.append(c)
            except queue.Empty:
                if self.p.poll() is not None:raise AssertionError(f'Process exited waiting for {text}: {self.pending}')
        pos=self.pending.index(text)+len(text);self.pending=self.pending[pos:]
    def send(self,text):self.p.stdin.write(str(text)+'\n');self.p.stdin.flush()
    def field(self,prompt,value):self.expect(prompt);self.send(value)
    def login(self,id):self.field('Choice: ',1);self.field('Employee ID: ',id);self.field('Password: ','ConsoleTest!');self.expect('DASHBOARD =====')
    def choice(self,n):self.field('Choice: ',n)
    def logout(self):self.choice(0);self.expect('STAFF LOGIN')
    def close(self):self.choice(0);self.p.stdin.close();assert self.p.wait(timeout=15)==0

a=App();a.field('Administrator name: ','Console Admin');a.field('Create password: ','ConsoleTest!');a.field('Confirm password: ','ConsoleTest!');a.login(1)
a.choice(5)
for id,name,role in [(2,'Manager','MANAGER'),(3,'Waiter','WAITER'),(4,'Chef','CHEF'),(5,'Cashier','CASHIER')]:
    a.choice(3);a.field('Employee ID: ',id);a.field('Name: ',name);a.field('CASHIER: ',role);a.field('Password: ','ConsoleTest!');a.field('Confirm: ','ConsoleTest!')
a.choice(0)
a.choice(6);a.field('Dish ID (existing updates): ',10);a.field('Name: ','Meal');a.field('Category: ','Main');a.field('Price: ','100.00');a.field('Menu preparation minutes: ',1);a.field('Available (yes/no): ','yes')
a.choice(7);a.field('Table number: ',1);a.field('Capacity: ',4)
a.logout();a.login(3);a.choice(8);a.field('Guests: ',2);a.expect('Order ID: 1001')
a.choice(9);a.field('Order ID: ',1001);a.field('Dish ID: ',10);a.field('Quantity: ',1);a.field('Special instructions: ','No chilli')
a.logout();a.login(4);a.choice(4);a.expect('Order 1001 item 1');a.logout()
# The next login/password hashing also allows the 10ms preparation to finish.
a.login(3);a.choice(10);a.field('Order ID: ',1001);a.field('Item number (1-based): ',1);a.choice(11);a.field('Order ID: ',1001);a.expect('Total: Rs. 105.00');a.logout()
a.login(2);a.choice(17);a.field('Order ID: ',1001);a.field('Discount percent 0..100: ',10);a.logout()
a.login(5)
for method,amount in [('CASH','40.00'),('UPI','54.50')]:
    a.choice(21);a.field('Order ID: ',1001);a.field('CASH / UPI / CARD: ',method);a.field('Amount: ',amount)
a.expect('Balance: Rs. 0.00');a.choice(22);a.field('Order ID: ',1001);a.logout()
a.login(3);a.choice(13);a.field('Cleaned table number: ',1);a.choice(2);a.expect('Status: AVAILABLE');a.logout()
a.login(5);a.choice(23);a.choice(1);a.expect('Pickup token / order ID: 1002');a.choice(2);a.field('Order ID: ',1002);a.field('Dish ID: ',10);a.field('Quantity: ',1);a.field('Special instructions: ','');a.choice(4);a.field('Order ID: ',1002);a.expect('Total: Rs. 105.00');a.choice(5);a.field('Order ID: ',1002);a.field('CASH / UPI / CARD: ','CARD');a.field('Amount: ','105.00');a.expect('Balance: Rs. 0.00')
# Wait on actual status, not a simulated success. Track until the worker finishes.
for attempt in range(30):
    a.choice(3);a.expect('Ready for collection: ')
    before=len(transcript)
    a.expect('TAKEAWAY WORKSPACE')
    if 'true' in ''.join(transcript[before:]):break
    time.sleep(.01)
a.choice(6);a.field('Order ID: ',1002);a.choice(0);a.choice(24);a.field('Order ID (0 for reports): ',0);a.expect('Cash receipts (includes tax and partial payments): 199.50');a.logout();a.close()
a=App();a.login(1);a.choice(18);a.expect('199.50');a.choice(3);a.expect('Status: CLOSED');a.logout();a.close()
text=''.join(transcript)
assert 'Operation failed:' not in text,text
(root/'console-transcript.txt').write_text(text)
print('PASS | Main console: first Admin, all role logins, staff/menu/table setup, seating, cooking, serving, discount, split payment, checkout, cleaning, takeaway, reports, process exit/restart and persisted totals')
print('PASS | No operation failures in captured console transcript')
