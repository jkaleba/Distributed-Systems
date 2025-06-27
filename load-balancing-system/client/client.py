import sys
import random
import threading
import LoadBalancer  # moduł wygenerowany przez slice2py
import Ice

ECHO_PROXY = "echo@EchoReplicaGroup"
CALC_PROXY = "calculator@EchoReplicaGroup"

def call_echo(proxy, idx):
    msg = f"Message-{idx}-from-{threading.get_ident()}"
    try:
        reply = proxy.echoString(msg)
        print(f"[Echo] Request {idx}: {reply}")
    except Exception as e:
        print(f"[Echo] Exception: {e}")

def call_calc(proxy, idx):
    a = random.randint(1, 100)
    b = random.randint(1, 100)
    try:
        result = proxy.add(a, b)
        print(f"[Calc] Request {idx}: {a} + {b} = {result}")
    except Exception as e:
        print(f"[Calc] Exception: {e}")

def run_requests(echo, calc, num_requests=100, threads=8):
    def worker(start_idx):
        for i in range(start_idx, start_idx + num_requests // threads):
            if random.random() < 0.5:
                call_echo(echo, i)
            else:
                call_calc(calc, i)

    thread_list = []
    for t in range(threads):
        th = threading.Thread(target=worker, args=(t * (num_requests // threads),))
        th.start()
        thread_list.append(th)
    for th in thread_list:
        th.join()

if __name__ == "__main__":
    with Ice.initialize(sys.argv) as communicator:
        echo_base = communicator.stringToProxy(ECHO_PROXY)
        calc_base = communicator.stringToProxy(CALC_PROXY)
        echo = LoadBalancer.EchoPrx.checkedCast(echo_base)
        calc = LoadBalancer.CalculatorPrx.checkedCast(calc_base)

        if not echo or not calc:
            print("Invalid proxy")
            sys.exit(1)

        run_requests(echo, calc, num_requests=200, threads=10)
