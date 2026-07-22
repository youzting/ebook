const libraries = [
  { id: 'simin', name: '부산시립시민도서관' },
  { id: 'busan-elib', name: '부산광역시 전자도서관' },
  { id: 'bukgu-ebook', name: '부산 북구 전자도서관' },
  { id: 'gangseo', name: '부산강서전자도서관' },
  { id: 'uos', name: '서울시립대 전자도서관' }
];

const sourceList = document.querySelector('#sourceList');
const chatForm = document.querySelector('#chatForm');
const chatInput = document.querySelector('#chatInput');
const conversation = document.querySelector('#conversation');
const resultsSection = document.querySelector('#resultsSection');
const resultsGrid = document.querySelector('#resultsGrid');
const linkResults = document.querySelector('#linkResults');
const sendButton = document.querySelector('#sendButton');
let latestResults = null;
let resultView = 'library';
let availableOnly = false;

function escapeHtml(value = '') {
  return value.replace(/[&<>'"]/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  })[character]);
}

function renderSources() {
  sourceList.innerHTML = libraries.map(library => `
    <label class="source-item">
      <input type="checkbox" value="${library.id}" checked>
      <span class="check">✓</span>
      <span class="source-name">${library.name}</span>
      <span class="source-dot"></span>
    </label>`).join('');
}

function selectedLibraries() {
  return [...sourceList.querySelectorAll('input:checked')].map(input => input.value);
}

function updateSelection() {
  const count = selectedLibraries().length;
  document.querySelector('#selectedCount').textContent = `${count}개 도서관`;
  document.querySelector('#toggleAll').textContent = count ? '전체 해제' : '전체 선택';
}

function addUserMessage(message) {
  conversation.insertAdjacentHTML('beforeend', `
    <article class="message user-message"><div class="bubble"><p>${escapeHtml(message)}</p></div></article>`);
}

function showTyping() {
  conversation.insertAdjacentHTML('beforeend', `
    <article id="typingMessage" class="message assistant-message">
      <div class="avatar">ㅁ</div><div class="bubble"><div class="typing"><span></span><span></span><span></span></div></div>
    </article>`);
}

function addAssistantMessage(message) {
  document.querySelector('#typingMessage')?.remove();
  conversation.insertAdjacentHTML('beforeend', `
    <article class="message assistant-message">
      <div class="avatar">ㅁ</div><div class="bubble"><p>${escapeHtml(message)}</p></div>
    </article>`);
}

function bookCard(book) {
  const cover = book.coverUrl
    ? `<img class="book-cover" src="${escapeHtml(book.coverUrl)}" alt="${escapeHtml(book.title)} 표지" loading="lazy" onerror="this.outerHTML='<div class=&quot;book-cover cover-fallback&quot;>표지 없음</div>'">`
    : `<div class="book-cover cover-fallback">${escapeHtml(book.title)}</div>`;
  const busy = book.availability === '대출 가능' ? '' : ' busy';
  return `<article class="book-card">
    ${cover}
    <div class="book-content">
      <span class="library-label">${escapeHtml(book.source.displayName)}</span>
      <h3>${escapeHtml(book.title)}</h3>
      <p class="book-meta">${escapeHtml(book.author || '저자 정보 없음')}</p>
      <p class="book-meta">${escapeHtml(book.publisher || '출판사 정보 없음')}</p>
      <div class="book-actions">
        <span class="availability${busy}">● ${escapeHtml(book.availability)}</span>
        <a class="book-link" href="${escapeHtml(book.detailUrl)}" target="_blank" rel="noopener">상세보기 ↗</a>
      </div>
    </div>
  </article>`;
}

function visibleBooks(data) {
  const books = availableOnly
    ? data.books.filter(book => book.availability === '대출 가능')
    : [...data.books];
  if (resultView === 'sequence') {
    books.sort((left, right) => left.title.localeCompare(right.title, 'ko'));
  }
  return books;
}

function renderBookCollection() {
  if (!latestResults) return;
  const books = visibleBooks(latestResults);
  document.querySelector('#resultCount').textContent = availableOnly
    ? `대출 가능 ${books.length}종 · 전체 ${latestResults.totalMatches}종`
    : `전체 ${latestResults.totalMatches}종`;

  if (!books.length) {
    resultsGrid.classList.remove('grouped');
    resultsGrid.innerHTML = '<div class="empty-results">조건에 맞는 전자책이 없습니다.</div>';
    return;
  }

  if (resultView === 'sequence') {
    resultsGrid.classList.remove('grouped');
    resultsGrid.innerHTML = books.map(bookCard).join('');
    return;
  }

  resultsGrid.classList.add('grouped');
  resultsGrid.innerHTML = libraries.map(library => {
    const libraryBooks = books.filter(book => book.source.id === library.id);
    if (!libraryBooks.length) return '';
    return `<section class="library-group">
      <div class="library-group-heading">
        <h3>${escapeHtml(library.name)}</h3><span>${libraryBooks.length}종</span>
      </div>
      <div class="library-group-grid">${libraryBooks.map(bookCard).join('')}</div>
    </section>`;
  }).join('');
}

function renderResults(data) {
  latestResults = data;
  resultsSection.hidden = false;
  document.querySelector('#resultsTitle').textContent = `“${data.keyword}” 검색 결과`;
  renderBookCollection();

  const links = data.libraries.filter(library => library.status !== 'CONNECTED' || !data.books.some(book => book.source.id === library.id));
  linkResults.innerHTML = links.map(library => `
    <a class="source-link" href="${escapeHtml(library.searchUrl)}" target="_blank" rel="noopener">
      <div><strong>${escapeHtml(library.name)}</strong><small>${escapeHtml(library.message)}</small></div>
      <span>공식 검색 ↗</span>
    </a>`).join('');
  resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function search(message) {
  const ids = selectedLibraries();
  if (!ids.length) {
    addAssistantMessage('검색할 도서관을 하나 이상 선택해 주세요.');
    return;
  }
  addUserMessage(message);
  showTyping();
  sendButton.disabled = true;
  chatInput.disabled = true;
  try {
    const response = await fetch('/api/v1/search/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, libraryIds: ids })
    });
    if (!response.ok) throw new Error('검색 요청에 실패했습니다.');
    const data = await response.json();
    addAssistantMessage(data.assistantMessage);
    renderResults(data);
  } catch (error) {
    document.querySelector('#typingMessage')?.remove();
    addAssistantMessage('검색 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.');
  } finally {
    sendButton.disabled = false;
    chatInput.disabled = false;
    chatInput.value = '';
    chatInput.style.height = 'auto';
    chatInput.focus();
  }
}

chatForm.addEventListener('submit', event => {
  event.preventDefault();
  const message = chatInput.value.trim();
  if (message) search(message);
});
chatInput.addEventListener('keydown', event => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    chatForm.requestSubmit();
  }
});
chatInput.addEventListener('input', () => {
  chatInput.style.height = 'auto';
  chatInput.style.height = `${Math.min(chatInput.scrollHeight, 140)}px`;
});
sourceList.addEventListener('change', updateSelection);
document.querySelector('#toggleAll').addEventListener('click', () => {
  const shouldCheck = selectedLibraries().length === 0;
  sourceList.querySelectorAll('input').forEach(input => { input.checked = shouldCheck; });
  updateSelection();
});
document.querySelector('.suggestions').addEventListener('click', event => {
  if (event.target.tagName === 'BUTTON') search(event.target.textContent.trim());
});
document.querySelector('.view-switch').addEventListener('click', event => {
  const button = event.target.closest('button[data-view]');
  if (!button) return;
  resultView = button.dataset.view;
  document.querySelectorAll('.view-switch button').forEach(item => {
    item.classList.toggle('active', item === button);
  });
  renderBookCollection();
});
document.querySelector('#availableOnly').addEventListener('change', event => {
  availableOnly = event.target.checked;
  renderBookCollection();
});

renderSources();
updateSelection();
