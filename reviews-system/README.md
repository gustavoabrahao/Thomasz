# Sistema de Avaliações - Thomasz Kowalski

Este sistema permite que os usuários enviem avaliações de estabelecimentos e que um administrador aprove ou rejeite essas avaliações antes de exibi-las no site.

## Estrutura do Sistema

```
reviews-system/
├── Review.java                # Classe modelo para as avaliações
├── ReviewManager.java         # Gerenciador de avaliações (DAO)
├── AuthManager.java           # Gerenciador de autenticação
├── ReviewServer.java          # Servidor HTTP
├── review-form.html           # Formulário de envio de avaliações
└── admin.html                 # Painel administrativo
```

## Tecnologias Utilizadas

- **Backend**: Java com servidor HTTP integrado (sem dependências externas)
- **Frontend**: HTML, CSS e JavaScript puro
- **Persistência**: Serialização Java para arquivos locais

## Recursos

### Para Usuários

- Formulário para envio de avaliações
- Feedback imediato sobre o envio da avaliação
- Classificação do estabelecimento (positiva ou negativa)
- Avaliação por estrelas (1 a 5)

### Para Administradores

- Painel de administração protegido por senha
- Visualização de avaliações pendentes
- Aprovação ou rejeição de avaliações
- Visualização das avaliações já aprovadas

## Como Executar

1. Compile os arquivos Java:

   ```
   javac -d . *.java
   ```

2. Execute o servidor:

   ```
   java com.thomaszkowalski.reviews.ReviewServer
   ```

3. Acesse o formulário de avaliações:

   ```
   http://localhost:8080/review-form.html
   ```

4. Acesse o painel administrativo:
   ```
   http://localhost:8080/admin.html
   ```

## Credenciais de Administrador

- **Usuário**: admin
- **Senha**: thomasz123

## Integração com o Site Principal

Para integrar este sistema com o site principal, adicione os seguintes links ao menu de navegação:

```html
<li><a href="/review-form.html">Enviar Avaliação</a></li>
```

E adicione o seguinte JavaScript ao arquivo `script.js` para carregar as avaliações aprovadas nas listas positivas e negativas:

```javascript
// Carregar avaliações aprovadas
function loadApprovedReviews() {
  fetch("http://localhost:8080/api/reviews?type=positive")
    .then((response) => response.json())
    .then((reviews) => {
      const positiveList = document.getElementById("positive-list");
      if (positiveList) {
        positiveList.innerHTML = "";

        if (reviews.length > 0) {
          reviews.forEach((review) => {
            const item = createReviewItem(review);
            positiveList.appendChild(item);
          });
        } else {
          positiveList.innerHTML =
            '<div class="list-empty-state"><p>Ainda não temos avaliações positivas. Seja o primeiro a contribuir!</p></div>';
        }
      }
    });

  fetch("http://localhost:8080/api/reviews?type=negative")
    .then((response) => response.json())
    .then((reviews) => {
      const negativeList = document.getElementById("negative-list");
      if (negativeList) {
        negativeList.innerHTML = "";

        if (reviews.length > 0) {
          reviews.forEach((review) => {
            const item = createReviewItem(review);
            negativeList.appendChild(item);
          });
        } else {
          negativeList.innerHTML =
            '<div class="list-empty-state"><p>Ainda não temos avaliações negativas. Ótima notícia!</p></div>';
        }
      }
    });
}

// Criar um item de avaliação para adicionar à lista
function createReviewItem(review) {
  const div = document.createElement("div");
  div.className = "list-item";

  const stars = "★".repeat(review.rating) + "☆".repeat(5 - review.rating);

  div.innerHTML = `
        <h4>${review.establishmentName}</h4>
        <p><strong>Endereço:</strong> ${review.address}</p>
        <p><strong>Avaliação:</strong> ${stars}</p>
        <p>${review.reviewText}</p>
        <p class="review-author">Por: ${review.reviewerName}</p>
    `;

  return div;
}

// Chamar esta função quando a página carregar
document.addEventListener("DOMContentLoaded", function () {
  loadApprovedReviews();
});
```

## Deploy no Vercel

Para fazer o deploy no Vercel com MongoDB Atlas:

1. Crie uma conta gratuita no [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)

   - Crie um novo cluster
   - Configure um usuário de banco de dados (ex: thomasz / [senha segura])
   - Adicione seu IP atual à lista de IPs permitidos
   - Obtenha a string de conexão em: Clusters > Connect > Connect your application

2. Crie uma conta na [Vercel](https://vercel.com) se ainda não tiver

3. Instale a CLI da Vercel (opcional, pode usar a interface web):

   ```
   npm install -g vercel
   ```

4. Faça upload do código para um repositório GitHub:

   ```bash
   git init
   git add .
   git commit -m "Versão para deploy"
   git remote add origin https://github.com/seu-usuario/seu-repositorio.git
   git push -u origin main
   ```

5. Importe o projeto na Vercel:

   - Acesse https://vercel.com/new
   - Importe do GitHub
   - Selecione seu repositório
   - Configure as variáveis de ambiente:
     - `MONGODB_URI` = sua string de conexão do MongoDB Atlas

6. Configure o build script:

   - Framework preset: Other
   - Build command: `cd reviews-system && mvn clean package`
   - Output directory: `reviews-system/target`

7. Clique em Deploy!

## Limitações e Problemas Comuns

- **Cold start**: As funções serverless sofrem com "cold start", o que pode tornar a primeira requisição mais lenta.
- **Logs**: Acesse os logs do Vercel para identificar problemas no seu sistema.
- **Permissões MongoDB**: Certifique-se que a string de conexão está correta e que o IP 0.0.0.0/0 (todos os IPs) está permitido na sua configuração de rede do MongoDB Atlas.

## Segurança

- As senhas são armazenadas com hash SHA-256
- Token simples para autenticação entre sessões
- Validação de entradas nos formulários

## Limitações e Melhorias Futuras

- Utilizar um banco de dados real (MySQL, PostgreSQL) em vez de arquivos
- Implementar autenticação JWT completa
- Adicionar testes automatizados
- Melhorar a validação de dados e segurança
