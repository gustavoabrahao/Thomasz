# Instruções de Configuração do Sistema

## 1. Vídeo Introdutório

O sistema está configurado para exibir um vídeo de introdução quando o usuário acessa o site pela primeira vez. Siga estas etapas para implementar o vídeo:

1. **Prepare seu vídeo de introdução**:

   - Formate o vídeo em MP4 (recomendado para compatibilidade)
   - Dimensões ideais: 1280x720 ou 1920x1080
   - Duração recomendada: 30-60 segundos
   - Tamanho máximo recomendado: 10MB

2. **Salve o vídeo no local correto**:

   - Nomeie o arquivo como `intro-video.mp4`
   - Coloque o arquivo na pasta `assets/`

3. **Teste o vídeo**:
   - O vídeo deve ser exibido automaticamente ao acessar o site
   - O usuário pode fechar o vídeo clicando no "X" no canto superior direito
   - O vídeo só é exibido uma vez por sessão de navegação

## 2. Sistema de Doação PIX

O sistema inclui um botão "Contribua com o Projeto" que exibe um QR Code PIX para computadores e a chave PIX com opção de cópia para dispositivos móveis.

1. **Configure a imagem do QR Code PIX**:

   - Gere o QR Code no seu aplicativo bancário
   - Salve a imagem como `qrcode-pix.png`
   - Coloque o arquivo na pasta `assets/`

2. **Configure a chave PIX**:

   - Abra o arquivo `index.html`
   - Localize a linha com `const pixKey = '0af67e28-dde8-4f2a-bd9a-109c9dbc06fd';`
   - Substitua a chave pelo seu valor real

3. **Adaptação automática**:
   - Em computadores: exibe o QR Code
   - Em celulares: exibe a chave para cópia

## 3. Configuração do MongoDB

O sistema está configurado para usar MongoDB Atlas para armazenamento persistente na Vercel:

1. **Crie uma conta no MongoDB Atlas**:

   - Acesse https://www.mongodb.com/cloud/atlas e registre-se
   - Crie um cluster (gratuito)

2. **Configure um usuário de banco de dados**:

   - Em "Database Access", crie um usuário e senha

3. **Configure as permissões de rede**:

   - Em "Network Access", permita conexões de 0.0.0.0/0 (qualquer IP)
   - Isso é necessário para permitir que a Vercel se conecte ao banco de dados

4. **Obtenha a string de conexão**:

   - Clique em "Connect" no seu cluster
   - Selecione "Connect your application"
   - Copie a string de conexão mostrada

5. **Configure a variável de ambiente no Vercel**:
   - Ao fazer deploy, adicione uma variável de ambiente chamada `MONGODB_URI`
   - Cole a string de conexão, substituindo `<password>` pela senha real

## 4. Deploy no Vercel

Após configurar os itens acima, siga estas etapas para fazer o deploy:

1. Faça upload do código para um repositório GitHub
2. Crie uma conta na Vercel e conecte-a ao GitHub
3. Importe o repositório
4. Configure a variável de ambiente `MONGODB_URI`
5. Configure o build:
   - Build command: `cd reviews-system && mvn clean package`
   - Output directory: `reviews-system/target`
6. Clique em "Deploy"

## Solução de Problemas

- **Vídeo não aparece**: Verifique se o arquivo está no local correto e no formato MP4
- **QR Code não aparece**: Verifique se a imagem está em `assets/qrcode-pix.png`
- **Erro de conexão com MongoDB**: Verifique as regras de IP no MongoDB Atlas e a senha na string de conexão
- **Avaliações não são salvas**: Consulte os logs da Vercel para identificar problemas com MongoDB
