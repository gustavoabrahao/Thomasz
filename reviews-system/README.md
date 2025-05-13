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

Para fazer o deploy no Vercel:

1. Adicione um arquivo `vercel.json` na raiz do projeto:

```json
{
  "version": 2,
  "builds": [
    { "src": "*.html", "use": "@vercel/static" },
    { "src": "api/src/main/java/**/*.java", "use": "@vercel/java" }
  ],
  "routes": [
    {
      "src": "/api/(.*)",
      "dest": "api/src/main/java/com/thomaszkowalski/api/ReviewServer.class"
    },
    { "src": "/(.*)", "dest": "/$1" }
  ]
}
```

2. Conecte seu repositório ao Vercel e faça o deploy.

## Segurança

- As senhas são armazenadas com hash SHA-256
- Token simples para autenticação entre sessões
- Validação de entradas nos formulários

## Limitações e Melhorias Futuras

- Utilizar um banco de dados real (MySQL, PostgreSQL) em vez de arquivos
- Implementar autenticação JWT completa
- Adicionar testes automatizados
- Melhorar a validação de dados e segurança
