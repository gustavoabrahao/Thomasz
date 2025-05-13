/**
 * Thomasz Kowalski Website - Main JavaScript
 */

document.addEventListener("DOMContentLoaded", () => {
  // Initialize all functions
  initScrollReveal();
  initMobileNav();
  initHeaderScroll();
  setCurrentYear();
  initListsContent();
  initHeroSequence();
});

/**
 * Handle fade-in animations on scroll
 */
function initScrollReveal() {
  const elements = document.querySelectorAll(".reveal-fade");

  // Set initial elements that are in view to active
  elements.forEach((element) => {
    if (
      isElementInViewport(element) &&
      !element.classList.contains("hero-element")
    ) {
      element.classList.add("active");
    }
  });

  // Handle scroll events
  window.addEventListener("scroll", () => {
    elements.forEach((element) => {
      if (
        isElementInViewport(element) &&
        !element.classList.contains("active") &&
        !element.classList.contains("hero-element")
      ) {
        element.classList.add("active");
      }
    });
  });
}

/**
 * Create a sequential reveal for hero elements
 */
function initHeroSequence() {
  const heroElements = document.querySelectorAll(".hero .reveal-fade");

  // Add a delay to each hero element for a sequential reveal
  setTimeout(() => {
    let delay = 300;
    heroElements.forEach((element, index) => {
      setTimeout(() => {
        element.classList.add("active");
      }, delay * index);
    });
  }, 500);
}

/**
 * Check if element is in viewport
 */
function isElementInViewport(element) {
  const rect = element.getBoundingClientRect();
  return (
    rect.top <=
    (window.innerHeight || document.documentElement.clientHeight) * 0.8
  );
}

/**
 * Handle mobile navigation
 */
function initMobileNav() {
  const hamburger = document.querySelector(".hamburger");
  const navLinks = document.querySelector(".nav-links");

  if (!hamburger || !navLinks) return;

  hamburger.addEventListener("click", () => {
    navLinks.classList.toggle("active");
    hamburger.classList.toggle("active");
  });

  // Close mobile menu when clicking on links
  document.querySelectorAll(".nav-links a").forEach((link) => {
    link.addEventListener("click", () => {
      navLinks.classList.remove("active");
      hamburger.classList.remove("active");
    });
  });
}

/**
 * Handle header style changes on scroll
 */
function initHeaderScroll() {
  const header = document.querySelector("header");

  if (!header) return;

  window.addEventListener("scroll", () => {
    if (window.scrollY > 100) {
      header.classList.add("scrolled");
    } else {
      header.classList.remove("scrolled");
    }
  });
}

/**
 * Set current year in footer copyright
 */
function setCurrentYear() {
  const yearElement = document.getElementById("current-year");
  if (yearElement) {
    yearElement.textContent = new Date().getFullYear();
  }
}

/**
 * Initialize content for the positive and negative lists
 * In a real application, this would come from an API or database
 */
function initListsContent() {
  // Carregar avaliações do servidor, se disponível
  loadApprovedReviews();

  // Se não foi possível carregar do servidor, usar exemplos
  setTimeout(() => {
    const positiveList = document.getElementById("positive-list");
    const negativeList = document.getElementById("negative-list");

    // Verificar se as listas já foram preenchidas pelo servidor
    if (positiveList && positiveList.children.length <= 1) {
      loadExampleReviews();
    }
  }, 1000);
}

/**
 * Carregar avaliações aprovadas do servidor
 */
function loadApprovedReviews() {
  // Tentar buscar do servidor
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
    })
    .catch((error) => {
      console.log("Erro ao carregar avaliações positivas:", error);
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
    })
    .catch((error) => {
      console.log("Erro ao carregar avaliações negativas:", error);
    });
}

/**
 * Criar item de avaliação para a lista
 */
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

/**
 * Carregar exemplos de avaliações (fallback)
 */
function loadExampleReviews() {
  // Example data - for demonstration purposes
  const positiveExamples = [
    {
      name: "Café Acessível",
      address: "Rua das Flores, 123 - Centro",
      description:
        "Atendimento excepcional, oferece cardápio em braile e funcionários treinados para atender PCDs com respeito e atenção.",
    },
    {
      name: "Restaurante Inclusivo",
      address: "Av. Central, 456 - Jardins",
      description:
        "Ambiente totalmente acessível, com rampas adequadas, mesas adaptadas e banheiros preparados para cadeirantes.",
    },
  ];

  const negativeExamples = []; // Leave this empty as requested - will show empty state

  // Functions to create list items
  const createPositiveItem = (item) => {
    return `
      <div class="list-item">
        <h4>${item.name}</h4>
        <p><strong>Endereço:</strong> ${item.address}</p>
        <p>${item.description}</p>
      </div>
    `;
  };

  const createNegativeItem = (item) => {
    return `
      <div class="list-item">
        <h4>${item.name}</h4>
        <p><strong>Endereço:</strong> ${item.address}</p>
        <p>${item.description}</p>
      </div>
    `;
  };

  // Populate lists
  const positiveList = document.getElementById("positive-list");
  const negativeList = document.getElementById("negative-list");

  if (positiveList && positiveExamples.length > 0) {
    positiveList.innerHTML = "";
    positiveExamples.forEach((item) => {
      positiveList.innerHTML += createPositiveItem(item);
    });
  }

  if (negativeList && negativeExamples.length > 0) {
    negativeList.innerHTML = "";
    negativeExamples.forEach((item) => {
      negativeList.innerHTML += createNegativeItem(item);
    });
  }
}

/**
 * Smooth scroll for anchor links
 */
document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
  anchor.addEventListener("click", function (e) {
    e.preventDefault();

    const targetId = this.getAttribute("href");
    const targetElement = document.querySelector(targetId);

    if (targetElement) {
      window.scrollTo({
        top: targetElement.offsetTop - 80,
        behavior: "smooth",
      });
    }
  });
});
