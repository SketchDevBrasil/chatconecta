# ChatConecta

Aplicativo Android (Java) de chat em tempo real, compatível com o **Gênesis Code AI**. Usa apenas dois serviços do Firebase:

- **Authentication** com e-mail e senha
- **Realtime Database**

## Funcionalidades

- **Splash screen** animada, que leva ao login ou direto ao app se já houver sessão.
- **Criar conta** (nome, e-mail, senha e confirmação) e **login** com e-mail e senha, com "Esqueci minha senha".
- **Chat público** para toda a comunidade, com faixa de perfis no topo.
- **Pessoas**: lista de perfis com busca por nome ou recado (ignora acentos e maiúsculas).
- **Tocar na foto** de alguém (no chat público, na faixa, na lista de pessoas ou nas conversas) abre o perfil com o botão **Abrir chat**.
- **Chat privado** com bolhas agrupadas, separador de datas, status online/visto por último, indicador "digitando…" e confirmação de leitura (✓ enviada, ✓✓ lida).
- **Conversas**: lista das conversas privadas com a última mensagem, horário e contador de não lidas (também aparece como selo na aba).
- **Meu perfil**: editar nome e recado e sair da conta.
- Tema claro e escuro automático. Funciona do Android 6 (API 23) ao Android 16 (API 36).

Nesta versão só há mensagens de texto. Como o Firebase Storage não é usado, a foto de perfil é um avatar com as iniciais do nome, sempre na mesma cor para cada pessoa.

## Configurar o Firebase

### 1. Criar o projeto e o app Android

1. Acesse o [Console do Firebase](https://console.firebase.google.com) e crie um projeto.
2. Adicione um app **Android** com o pacote `com.my.chatconecta`.
3. Baixe o `google-services.json`. Ele não vai para o projeto, só servirá para copiar os valores no passo 4.

### 2. Ativar o login por e-mail e senha

Em **Authentication > Método de login**, ative **E-mail/senha**.

### 3. Criar o Realtime Database e publicar as regras

1. Em **Realtime Database**, clique em **Criar banco de dados** e escolha a região.
2. Na aba **Regras**, apague o conteúdo e cole todo o arquivo `database.rules.json` deste projeto. Clique em **Publicar**.

Se preferir usar a Firebase CLI, troque `SEU-PROJECT-ID` em `.firebaserc` pelo id do seu projeto e rode:

```powershell
firebase login
firebase deploy --only database
```

### 4. Colar as credenciais no app

O builder do Gênesis não executa o plugin `google-services`, por isso as credenciais ficam em código. Abra `app/src/main/java/com/my/chatconecta/data/FirebaseInit.java` e substitua os valores de exemplo pelos do `google-services.json`:

| Constante | Campo no google-services.json |
| --- | --- |
| `APPLICATION_ID` | `client[0].client_info.mobilesdk_app_id` |
| `API_KEY` | `client[0].api_key[0].current_key` |
| `PROJECT_ID` | `project_info.project_id` |
| `SENDER_ID` | `project_info.project_number` |
| `DATABASE_URL` | `project_info.firebase_url` |

Se o `firebase_url` não aparecer no arquivo (acontece quando o JSON foi baixado antes de criar o banco), copie a URL do topo da tela do Realtime Database. Fora dos EUA, ela tem o formato `https://SEU-PROJETO-default-rtdb.REGIAO.firebasedatabase.app`.

Enquanto os valores de exemplo estiverem no arquivo, a splash mostra o aviso **"Firebase ainda não configurado"** em vez de abrir o app.

## Compilar

**No Gênesis Code:** abra a pasta do projeto e rode `android build`. As dependências já estão baixadas em `app/libs`, igual ao projeto Herbalux.

**No Android Studio ou pelo Gradle:** abra a pasta normalmente. É necessário usar o JDK 17.

## Estrutura do banco

```
usuarios/{uid}
    nome, nomeBusca, recado, criadoEm, online, vistoEm
chatPublico/{mensagemId}
    uid, nome, texto, enviadoEm
conversas/{uidA_uidB}                  (uids em ordem alfabética)
    mensagens/{mensagemId}: uid, texto, enviadoEm
    digitando/{uid}: true
    lidoEm/{uid}: horário da última leitura
usuarioConversas/{uid}/{contatoUid}
    ultimaMensagem, ultimaEm, ultimoUid, naoLidas

limites/{uid}/{a|b|c|d|e}
    id, em                  5 vagas do limite de 5 mensagens por minuto
```

O que as regras de `database.rules.json` garantem:

- Só usuários logados leem perfis e o chat público.
- Cada usuário edita apenas o próprio perfil. O e-mail não é gravado no perfil público.
- Ninguém envia mensagem em nome de outra pessoa, e mensagens não podem ser editadas nem apagadas.
- No chat público, o nome da mensagem precisa ser igual ao nome do perfil de quem enviou.
- Uma conversa privada só pode ser lida e escrita pelos dois participantes (os dois uids que formam o id).
- O contador de não lidas só pode subir de 1 em 1 (pelo contato) ou ser zerado (pelo dono).
- Mensagens têm no máximo 500 caracteres e não podem ser só espaços.
- **Cada usuário envia no máximo 5 mensagens por minuto**, somando chat público e privado.
  Como as regras do Realtime Database não sabem contar filhos de um nó, cada usuário tem
  5 vagas fixas em `limites/{uid}/{a..e}`. Toda mensagem exige uma vaga
  `{ id: <id da mensagem>, em: <horário do servidor> }` gravada na **mesma escrita atômica**,
  e uma vaga só pode ser reusada 60s depois — apagar a vaga é proibido. Como a vaga guarda o
  id da mensagem, um lote com mil mensagens precisaria de mil vagas: só existem 5, então a
  escrita inteira é recusada. Quem escolhe a vaga é `data/Limite.java`; quem impõe o limite
  é o servidor.
- Todos os horários (`enviadoEm`, `ultimaEm`, `lidoEm`, `vistoEm`, `criadoEm`) são gravados
  pelo servidor: o cliente não consegue mandar horário falso.
- O id de cada mensagem tem de ser uma chave `push()`, e conversa só existe com um uid
  que esteja em `/usuarios`.

## Organização do código

```
app/src/main/java/com/my/chatconecta/
    ChatApp.java             Inicia o Firebase e controla online/offline
    SplashActivity.java      Splash animada
    LoginActivity.java       Login e "Esqueci minha senha"
    CriarcontaActivity.java  Cadastro
    MainActivity.java        Abas Público, Conversas e Pessoas
    ChatActivity.java        Chat privado
    PerfilActivity.java      Editar perfil e sair
    adapter/                 MensagemAdapter, PessoaAdapter, ConversaAdapter
    data/                    FirebaseInit, Banco (caminhos e escritas), Presenca, Sessao
    model/                   Usuario, Mensagem, Conversa
    ui/                      PerfilSheet (folha "Abrir chat"), BotaoEnviar
    util/                    Avatar, Tempo, Texto, Janela (barras do sistema e teclado)
```

## Limites conhecidos

- A tela principal carrega todos os perfis de uma vez. Isso funciona bem para comunidades de algumas centenas de pessoas. Para milhares, troque por uma busca paginada usando `orderByChild("nomeBusca")`, que já está indexado nas regras.
- O chat público carrega as últimas 150 mensagens e o privado as últimas 300.
- A confirmação de leitura mostra só "lida" ou "enviada". O app não mostra quando a mensagem ainda está na fila, sem internet.
