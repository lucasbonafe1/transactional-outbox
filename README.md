# Aplicação Ecommerce - Transactional Outbox Pattern
Um serviço de pedidos Java + Spring Boot, com PostgreSQL, um processo de polling que publica no Kafka, e dois consumidores (estoque e notificação).
<img width="1836" height="781" alt="image" src="https://github.com/user-attachments/assets/1b25b62e-fa56-4d5b-8866-8a7daba4e248" />

## Sobre o Padrão:
### Para que é utilizado?
- Para evitar dual write no processo da aplicação. O padrão garante que um evento é publicado somente se a operação de negócio foi confirmada.

### Como funciona?
- Em uma mesma transação, a aplicação grava o dado na tabela da entidade, e também, insere o evento, com um ID único, em uma outra tabela chamada Outbox, sendo que, ou as duas gravações são feitas, ou nenhuma. Ao salvar com sucesso, um processo em background (polling ou CDC) lê a tabela Outbox com os eventos pendentes, e publica no broker de mensageria em ordem. E quando o broker confirma o recebimento (ack), o evento é marcado com publicado e removido da tabela Outbox.

### E caso a publicação do evento falhe?
- A operação de negocio não é desfeita, o evento continua na tabela Outbox, e é publicado em uma outra tentativa

### Garantia de entrega do evento: 
- at-least-once (padrão dos brokers atuais de mensageria, como Kafka e RabbitMQ). Se o processo cair entre publicar e marcar o evento, ou se o ack se perder, o evento é publicado de novo.

### Garantir Idempotência:
- Como o broker de mensagens garante a entrega no modelo at-least-once (pelo menos uma vez), cenários de falhas de rede ou reinicializações farão com que a mesma mensagem seja enviada mais de uma vez, e com isso, precisamos garantir a idempotência (garantir que processar a mensagem várias vezes resulte no mesmo estado que processá-la apenas uma vez), a responsabilidade é transferida para o serviço consumidor. 

### As formas mais comuns são:
- registrar os IDs de eventos já processados e ignorar repetidos (padrão Inbox);
- usar operações naturalmente idempotentes, como upsert ou “definir status = "CONCLUÍDO"” em vez de um novo insert;
- verificar a versão da entidade antes de aplicar a mudança.
