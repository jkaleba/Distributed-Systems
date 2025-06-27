import pika
import time

def main():
    connection = pika.BlockingConnection(pika.ConnectionParameters("localhost"))
    channel = connection.channel()
    channel.exchange_declare(exchange="admin", exchange_type="topic")

    queue = "admin"
    channel.queue_declare(queue=queue)
    channel.queue_bind(exchange="admin", queue=queue, routing_key="#")

    def callback(ch, method, properties, body):
        print("[ADMIN] PODGLĄD:", body.decode())

    channel.basic_consume(queue=queue, on_message_callback=callback, auto_ack=True)

    messages = [
        ("ADMIN: Do wszystkich Ekip!", "admin.team"),
        ("ADMIN: Do wszystkich Dostawców!", "admin.supplier"),
        ("ADMIN: Do wszystkich Ekip i Dostawców!", "admin.all"),
    ]

    print("[ADMIN] Rozpoczynam wysyłanie komunikatów...")
    while True:
        for msg, rk in messages:
            channel.basic_publish(exchange="admin", routing_key=rk, body=msg)
            print(f"[ADMIN] Wysłano: {msg} (routing_key={rk})")
            time.sleep(10)

if __name__ == "__main__":
    main()
