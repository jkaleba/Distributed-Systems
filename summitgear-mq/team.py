import pika
import sys

TEAM_NAME = sys.argv[1]

def callback(ch, method, properties, body):
    message = body.decode()
    if message.startswith("ADMIN:"):
        print(f"[{TEAM_NAME}] ODEBRANO WIADOMOŚĆ OD ADMINA: {message}")
    else:
        print(f"[{TEAM_NAME}] ODEBRANO POTWIERDZENIE: {message}")

def main():
    connection = pika.BlockingConnection(pika.ConnectionParameters("localhost"))
    channel = connection.channel()
    channel.exchange_declare(exchange="orders", exchange_type="direct")
    channel.exchange_declare(exchange="confirmations", exchange_type="direct")
    channel.exchange_declare(exchange="admin", exchange_type="topic")

    queue = f"team.{TEAM_NAME}"
    channel.queue_declare(queue=queue)
    channel.queue_bind(exchange="admin", queue=queue, routing_key="admin.team")
    channel.queue_bind(exchange="admin", queue=queue, routing_key="admin.all")

    channel.queue_bind(exchange="confirmations", queue=queue, routing_key=TEAM_NAME)

    channel.basic_consume(queue=queue, on_message_callback=callback, auto_ack=True)

    if TEAM_NAME == "ekipa1":
        orders = ["tlen", "tlen", "buty", "buty", "plecak", "plecak"]
        for equipment in orders:
            print(f"[{TEAM_NAME}] WYSŁANO ZAMÓWIENIE: {equipment}")
            channel.basic_publish(
                exchange="orders",
                routing_key=equipment,
                body=f"{TEAM_NAME}|{equipment}"
            )

    print(f"[{TEAM_NAME}] Oczekiwanie na wiadomości...")
    channel.start_consuming()

if __name__ == "__main__":
    main()
