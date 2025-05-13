# Instruções de Configuração do Sistema

## 1. Sistema de Doação PIX

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

## 2. Configuração do MongoDB

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

## 3. Deploy no Vercel

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

- **QR Code não aparece**: Verifique se a imagem está em `assets/qrcode-pix.png`
- **Erro de conexão com MongoDB**: Verifique as regras de IP no MongoDB Atlas e a senha na string de conexão
- **Avaliações não são salvas**: Consulte os logs da Vercel para identificar problemas com MongoDB
