# HTTPServer
Implementação de servidor HTTP simplíssimo em Java.

Feito como exercício de revisão de Sockets para a matéria de redes, da prof. 
Patrícia.

## Primeiros passos

### Para rodar
Para compilar o projeto, digite num prompt de comando na pasta do projeto:
```
javac WebServer.java
```
E para executar:
```
java WebServer
```

### Para usar
Por padrão, o servidor é aberto na porta 80 da máquina. É possível acessar os
arquivos do servidor normalmente pelo browser, digitando `localhost` na barra 
de endereço, seguido pelo nome do arquivo (e.g. `localhost/index.html`).

Para tornar visível um arquivo pelo servidor, coloque-o numa pasta chamada 
`htdocs`, criada na mesma pasta onde o servidor está sendo executado.