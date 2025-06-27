import pika
import sys

SUPPLIER_NAME = sys.argv[1]
TYPES = sys.argv[2].split(",")
order_id = 1

def callback(ch, method, properties, body):
    global order_id
    msg = body.decode()
    if msg.startswith("ADMIN:"):
        print(f"[{SUPPLIER_NAME}] ODEBRANO WIADOMOŚĆ OD ADMINA: {msg}")
    else:
        team, equipment = msg.split("|")
        print(f"[{SUPPLIER_NAME}] OTRZYMANO ZAMÓWIENIE od {team}: {equipment} (zlecenie {order_id})")
        confirm_msg = f"Zlecenie {order_id} na {equipment} zrealizowane przez {SUPPLIER_NAME}"
        channel.basic_publish(
            exchange="confirmations",
            routing_key=team,
            body=confirm_msg
        )
        print(f"[{SUPPLIER_NAME}] WYSŁANO POTWIERDZENIE do {team}: {confirm_msg}")
        order_id += 1

def main():
    global order_id, channel
    connection = pika.BlockingConnection(pika.ConnectionParameters("localhost"))
    channel = connection.channel()
    channel.exchange_declare(exchange="orders", exchange_type="direct")
    channel.exchange_declare(exchange="confirmations", exchange_type="direct")
    channel.exchange_declare(exchange="admin", exchange_type="topic")

    queue = f"supplier.{SUPPLIER_NAME}"
    channel.queue_declare(queue=queue)
    channel.queue_bind(exchange="admin", queue=queue, routing_key="admin.supplier")
    channel.queue_bind(exchange="admin", queue=queue, routing_key="admin.all")

    for t in TYPES:
        channel.queue_bind(exchange="orders", queue=queue, routing_key=t)

    print(f"[{SUPPLIER_NAME}] Obsługiwane typy: {TYPES}")
    channel.basic_consume(queue=queue, on_message_callback=callback, auto_ack=True)
    channel.start_consuming()

if __name__ == "__main__":
    main()