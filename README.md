# ExoKotlinAudioPoc
Prova de conceito de Audio player usando kotlin e exoplayer // POC  about exoPlayer with Kotlin

Nesse projeto é construido um player multimida que executa em backgorund atraves de uma notificação.

Fontes de estudo // References

Conference Google IO/18 
https://www.youtube.com/watch?v=svdq1BWl4r8&list=PLxek1l0eFOhp_J3kgww3uGALBEtc6xDha&index=4&t=0s

Starter Path CodeLabs
https://codelabs.developers.google.com/codelabs/exoplayer-intro/#0

ExoPlayer Documentation
https://exoplayer.dev/doc/reference/overview-summary.html

Nesse projeto foi utilizado a biblioteca ExoPlayer da google, para criar um player de midia que funciona em background e também com o celular em standby. 

*O que foi feito:*
- Foi criado um serviço para executar o player em background através de uma notificação:
  Para o android conseguir reproduzir algo com o app minimizado ou com o dispositivo em standby é necessario criar um serviço e           inicializar esse serviço em foreground, isso pode ser feito ao inicializar uma notificação(como foi feito no arquivo        
  AudioPlayerService.kt, no metodo onNotificationPosted, linha 137) e deve ser encerrado quando a aplicação for encerrada (que tambem 
  pode ser feito atrelado a vida util da notificação, só que no caso no onNotificationCancelled, chamando o metodo stopSelf, linha 127).
  Com esse serviço e um NotificationManager é possivel controlar o audioPlayer em background através, de uma notificação fixa que será 
  criada no app.
  
  
- Foi criada uma view para o player:
  Essa view nada mais é do que uma activity com o elemento PlayerView no XML, esse elemento pode ser praticamente todo cutomizado com um outro XML que deve ser adicionado em sua propriedade "app:controller_layout_id" como um layout.
  Como dito antes essa player view é altamente customizavel e pode ser alterada, cores, icones e até mesmo algumas funcionalidades, como quanto tempo o fastfoward avança em milesegundos. Esse tipo de customização pode ser feito via codigo tambem, através do ID da view do player. Ex: de customização => https://codelabs.developers.google.com/codelabs/exoplayer-intro/#6
  
  
- Foi conectado o serviço ao player
  Bom depois de ter um serviço e uma uma PlayerView é necessario conectar os dois, se não a view não influenciara no player. Essa pode ser a parte mais chatinha de ser feita, mas vamos por partes.
  Primeiro foi necessario criar uma inner class, dentro da classe do AudioPlayerService, essa classe chamada no projeto de LocalBinder, estende Binder() e tem um metodo retorna um AudioPlayerService para uma variavel do tipo IBinder. Depois disso é necessario colocar essa varial no retorno do método onBind do AudioPlayerService. Feito isso deve ser criado um metodo para retornar a instância do player para a view, linha 186 no AudioPlayerService.kt . Por fim na nossa activity é necessario criar uma conexaão com o serviço, linha 20 e inicializar o player criado na AudioPlayerService.kt com a playerView, linha 33.
  
*Extras:* 
  - Controle de foco de audio
  - MediaSession (Ok, google)
----------------------------------------------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------------------------------------------

*O que faltou:*
- Fazer a notificação ser clicavel e ao clickar abrir o app;
- Melhorar o controle da seekBar(barra de progresso da faixa de audio/video).
- Aprender a usar os estado do player de forma efetiva. Estados(IDLE, BUFFERING, READY, ENDED, UNKNOW).
- Validar uso de variaveis locais (usando a biblioteca Hawk, talvez?) para controlar a faixa atual e o tempo atual da mesma.
- Validar se esse jeito é o melhor para implentear um serviço.
- Entender melhor os limites das costumizações de layout da playerView.
