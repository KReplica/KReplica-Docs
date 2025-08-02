import {themeSwitcher} from './components/theme-switcher.js';
import * as playground from './pages/playground.js';

window.KREPLICA_PLAYGROUND = playground;

window.generateUniqueId = function () {
    if (crypto.randomUUID) return crypto.randomUUID();
    return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
        (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
};

async function initializeApp() {
    if (document.querySelector('[data-js-id="guide-sidebar-links"]')) {
        const guide = await import('./pages/guide.js');
        guide.init();
    }
    if (document.getElementById('kreplica-editor')) {
        playground.init();
    }
}

document.addEventListener('alpine:init', () => {
    Alpine.data('themeSwitcher', themeSwitcher);
});

document.addEventListener('DOMContentLoaded', () => {
    void initializeApp();
});

document.body.addEventListener('htmx:configRequest', evt => {
    if (evt.detail.path === '/playground/compile' && window.KREPLICA_PLAYGROUND) {
        const editor = window.KREPLICA_PLAYGROUND.getEditorInstance();
        if (editor) evt.detail.parameters['source'] = editor.getValue();
    }
});

const afterSwapTasks = (elt) => {
    Prism.highlightAllUnder(elt);
    void initializeApp();
};

const handleOutputReady = elt => {
    if (elt.id === 'playground-output') window.dispatchEvent(new CustomEvent('output-ready'));
};

document.body.addEventListener('htmx:afterSwap', e => {
    const elt = e.detail.elt;
    afterSwapTasks(elt);
    handleOutputReady(elt);
});

document.body.addEventListener('htmx:oobAfterSwap', e => {
    const elt = e.detail.elt;
    afterSwapTasks(elt);
    handleOutputReady(elt);
});

document.body.addEventListener('htmx:afterSettle', async () => {
    if (document.querySelector('[data-js-id="guide-sidebar-links"]')) {
        const {initScrollSpy} = await import('./components/scroll-spy.js');
        initScrollSpy();
    }
});

document.body.addEventListener('htmx:beforeSwap', () => {
    const target = event?.detail?.target;
    if (target?.classList?.contains('main-content')) {
        import('./pages/playground.js').then(p => {
            if (p.getEditorInstance()) p.disposeEditor();
        });
    }
});