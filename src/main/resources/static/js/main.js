import {themeSwitcher} from './components/theme-switcher.js';
import * as playground from './pages/playground.js';

window.KREPLICA_PLAYGROUND = playground;

function log(message, ...args) {
    console.log(`[KREPLICA-DEBUG] ${new Date().toLocaleTimeString()} - ${message}`, ...args);
}

log("main.js script loaded.");

window.generateUniqueId = function () {
    if (crypto.randomUUID) {
        return crypto.randomUUID();
    }
    return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
        (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
};

async function initializeApp() {
    log("initializeApp() called.");
    if (document.querySelector('[data-js-id="guide-sidebar-links"]')) {
        log("Guide page detected, initializing guide.js");
        const guide = await import('./pages/guide.js');
        guide.init();
    }

    if (document.getElementById('kreplica-editor')) {
        log("Playground detected, initializing playground.js");
        playground.init();
    }
}

document.addEventListener('alpine:init', () => {
    log("alpine:init event triggered.");
    Alpine.data('themeSwitcher', themeSwitcher);
});

document.addEventListener('DOMContentLoaded', () => {
    log("DOMContentLoaded event triggered.");
    void initializeApp();
});


document.body.addEventListener('htmx:sseOpen', function (evt) {
    log('EVENT -> htmx:sseOpen', {element: evt.detail.elt});
});

document.body.addEventListener('htmx:sseMessage', function (evt) {
    log('EVENT -> htmx:sseMessage', {type: evt.detail.type, data: evt.detail.data});
});

document.body.addEventListener('htmx:sseError', function (evt) {
    log('EVENT -> htmx:sseError', {error: evt.detail.error, xhr: evt.detail.xhr});
});

document.body.addEventListener('htmx:sseClose', function (evt) {
    log('EVENT -> htmx:sseClose', {element: evt.detail.elt});
});


function afterSwapTasks(elt, swapType) {
    log(`afterSwapTasks called for ${swapType}`, {element: elt});
    Prism.highlightAllUnder(elt);
    void initializeApp();
}

document.body.addEventListener('htmx:afterSwap', (e) => {
    const elt = e.detail.elt;
    log('EVENT -> htmx:afterSwap', {target: elt, path: e.detail.pathInfo.path});
    afterSwapTasks(elt, 'htmx:afterSwap');

    if (elt.id === 'editor-source-container') {
        log('Swap was for editor-source-container, handling editor update.');
        import('./pages/playground.js').then(playground => {
            const editor = playground.getEditorInstance();
            if (editor) {
                const newSource = elt.querySelector('textarea[name="source"]').value;
                const alpineComponent = document.querySelector('.playground-container').__x;
                if (alpineComponent) {
                    alpineComponent.data.isOutputReady = false;
                }
                if (playground.setEditorModelFromSource) {
                    playground.setEditorModelFromSource(newSource);
                } else {
                    playground.setEditorValue(newSource);
                }
            }
            if (playground.clearPlaygroundOutput) {
                playground.clearPlaygroundOutput();
            }
        });
    }
});

document.body.addEventListener('htmx:oobAfterSwap', (e) => {
    const elt = e.detail.elt;
    log('EVENT -> htmx:oobAfterSwap', {target: elt});
    afterSwapTasks(elt, 'htmx:oobAfterSwap');

    if (elt.id === 'playground-output') {
        log('OOB Swap was for playground-output, handling SSE cleanup.');
        window.dispatchEvent(new CustomEvent('output-ready'));
        const sseElement = document.querySelector('[sse-connect]');
        if (sseElement) {
            log('Found SSE element, triggering htmx:sseClose and removing element.', sseElement);
            htmx.trigger(sseElement, 'htmx:sseClose');
            sseElement.remove();
        } else {
            log('OOB Swap for playground-output, but NO [sse-connect] element was found on the page.');
        }
    }
});

document.body.addEventListener('htmx:afterSettle', async (e) => {
    log('EVENT -> htmx:afterSettle', {target: e.target});
    if (document.querySelector('[data-js-id="guide-sidebar-links"]')) {
        const {initScrollSpy} = await import('./components/scroll-spy.js');
        initScrollSpy();
    }
});

document.body.addEventListener('htmx:beforeSwap', (evt) => {
    log('EVENT -> htmx:beforeSwap', {target: evt.detail.target});
    const target = evt.detail.target;
    if (target && target.classList && target.classList.contains('main-content')) {
        log("Main content is being swapped out, disposing editor if it exists.");
        import('./pages/playground.js').then(playground => {
            if (playground.getEditorInstance()) {
                playground.disposeEditor();
            }
        });
    }
});