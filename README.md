# Redes-projeto

Este projeto consiste em um protocolo de transporte confiável teórico, acima do UDP, considerando que o TCP ainda não havia sido criado. Para tal, usou-se o protocolo Go-Back-N como base para o tratamento de mensagens.

Basicamente, o sistema tem as seguintes opções de envio: Fora de Ordem, Perda, Duplicada, Atrasada e Normal.

## Funcionalidades

### Fora de ordem

Caso essa opção seja escolhida, o programa envia a mensagem após a mensagem seguinte.

### Perda

Caso essa opção seja escolhida, o programa não envia a mensagem.

### Duplicada

Caso essa opção seja escolhida, a mensagem é enviada duas vezes seguidas.

### Atrasada

Caso essa opção seja escolhida, a mensagem é enviada com Delay.

### Normal

 Caso essa opção seja escolhida, a mensagem é enviada normalmente.