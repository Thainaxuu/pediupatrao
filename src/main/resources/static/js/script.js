// Alternar sidebar em telas pequenas
document.addEventListener('DOMContentLoaded', () => {
    const botao = document.querySelector('.toggle-btn');
    if (botao) {
        botao.addEventListener('click', () => {
            document.getElementById('sidebar').classList.toggle('active');
        });
    }
});

// Alternar submenu
function toggleSubmenu(event, element) {
    event.preventDefault();
    element.parentElement.classList.toggle('open');
}

/** Cabecalho CSRF lido das meta tags do layout (CSRF esta habilitado no Spring Security). */
function csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    if (!token || !header) return {};
    return { [header.content]: token.content };
}

/**
 * fetch() padronizado para a API: envia JSON, inclui o token CSRF e transforma
 * respostas de erro ({mensagem, detalhes}) em excecoes com mensagem legivel.
 */
async function apiFetch(url, options = {}) {
    const headers = Object.assign(
        { 'Accept': 'application/json' },
        options.body ? { 'Content-Type': 'application/json' } : {},
        csrfHeaders(),
        options.headers || {}
    );
    const resposta = await fetch(url, Object.assign({}, options, { headers, credentials: 'same-origin' }));
    const texto = await resposta.text();
    let dados = null;
    if (texto) {
        try { dados = JSON.parse(texto); } catch (e) { dados = texto; }
    }
    if (!resposta.ok) {
        let mensagem = 'Erro ' + resposta.status;
        if (resposta.status === 401) mensagem = 'Sessão expirada. Faça login novamente.';
        if (dados && dados.mensagem) {
            mensagem = dados.mensagem;
            if (Array.isArray(dados.detalhes) && dados.detalhes.length) {
                mensagem += '\n' + dados.detalhes.join('\n');
            }
        }
        const erro = new Error(mensagem);
        erro.status = resposta.status;
        erro.dados = dados;
        throw erro;
    }
    return dados;
}

/** Evita XSS ao montar HTML com dados vindos da API. */
function escapeHtml(valor) {
    if (valor === null || valor === undefined) return '';
    return String(valor)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function formatarMoeda(valor) {
    const n = Number(valor || 0);
    return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatarNumero(valor, casas = 0) {
    return Number(valor || 0).toLocaleString('pt-BR', { minimumFractionDigits: casas, maximumFractionDigits: casas });
}
