import {getMonacoTheme} from '../utils/theme.js';
import {getCompletionProvider} from '../components/completion-provider.js';

let kreplicaEditor = null;
let cleanupFns = [];
let resizeObserver = null;
let outputObserver = null;
let isInitializing = false;
let languageModel = null;

const MOBILE_BREAKPOINT_PX = 992;

const ACTIONS = {
    RUN: 'run',
    RESET_ALL: 'reset-all',
    CLEAR_OUTPUT: 'clear-output'
};

const isMobile = () => window.innerWidth < MOBILE_BREAKPOINT_PX;

const getHiddenTextarea = () => document.querySelector('textarea[name="source"]');

function isElementVisibleAndSized(el) {
    if (!el) return false;
    const rect = el.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0 && el.offsetParent !== null;
}

function waitForVisibleSize(el, ready) {
    const check = () => {
        if (isElementVisibleAndSized(el)) {
            ready();
        } else {
            requestAnimationFrame(check);
        }
    };
    requestAnimationFrame(check);
}

function resizeEditorToContent() {
    if (!kreplicaEditor) return;
    const editorNode = document.getElementById('kreplica-editor');
    if (!editorNode) return;
    editorNode.style.height = '';
    if (isMobile()) {
        editorNode.style.height = '100%';
        kreplicaEditor.layout();
        return;
    }
    const height = kreplicaEditor.getContentHeight();
    editorNode.style.height = height + 'px';
    const width = editorNode.clientWidth || editorNode.parentElement?.clientWidth || 0;
    kreplicaEditor.layout({width, height});
}

function initOutputObserver() {
    if (outputObserver) outputObserver.disconnect();
    const outputNode = document.getElementById('playground-output');
    if (!outputNode) return;
    const observerCallback = mutationsList => {
        for (const mutation of mutationsList) {
            if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                outputObserver.disconnect();
                Prism.highlightAllUnder(outputNode);
                outputObserver.observe(outputNode, {childList: true, subtree: true});
                break;
            }
        }
    };
    outputObserver = new MutationObserver(observerCallback);
    outputObserver.observe(outputNode, {childList: true, subtree: true});
    cleanupFns.push(() => {
        if (outputObserver) {
            outputObserver.disconnect();
            outputObserver = null;
        }
    });
}

function initKReplicaPlayground() {
    if (kreplicaEditor || isInitializing) return;
    const editorNode = document.getElementById('kreplica-editor');
    const inputColumn = document.querySelector('.playground-input-column');
    if (!editorNode || !inputColumn) return;
    isInitializing = true;

    const start = () => {
        require.config({paths: {vs: 'https://unpkg.com/monaco-editor@0.52.2/min/vs'}});
        require(['vs/editor/editor.main'], async () => {
            try {
                const res = await fetch('/language-model.json');
                if (!res.ok) throw new Error(`Failed to load: ${res.statusText}`);
                languageModel = await res.json();
            } catch (e) {
                console.error('Failed to load KReplica language model:', e);
                languageModel = null;
            }

            const hiddenTextareaEl = getHiddenTextarea();
            const initialCode = hiddenTextareaEl?.value || '';
            const currentSiteTheme = document.documentElement.getAttribute('data-theme') || 'light';
            const initialMonacoTheme = getMonacoTheme(currentSiteTheme);
            kreplicaEditor = monaco.editor.create(editorNode, {
                value: initialCode,
                language: 'kotlin',
                automaticLayout: true,
                theme: initialMonacoTheme,
                minimap: {enabled: false},
                folding: true,
                scrollBeyondLastLine: false,
                scrollbar: {vertical: isMobile() ? 'auto' : 'hidden'},
                lineHeight: 20
            });

            monaco.languages.registerCompletionItemProvider('kotlin', getCompletionProvider(languageModel));

            kreplicaEditor.onDidChangeModelContent(() => {
                const currentTextarea = getHiddenTextarea();
                if (currentTextarea) currentTextarea.value = kreplicaEditor.getValue();
            });

            kreplicaEditor.onDidContentSizeChange(resizeEditorToContent);
            resizeEditorToContent();

            if (resizeObserver) resizeObserver.disconnect();
            resizeObserver = new ResizeObserver(resizeEditorToContent);
            resizeObserver.observe(inputColumn);
            cleanupFns.push(() => {
                if (resizeObserver) {
                    resizeObserver.disconnect();
                    resizeObserver = null;
                }
            });

            const onVisibility = () => {
                if (document.visibilityState === 'visible') resizeEditorToContent();
            };
            document.addEventListener('visibilitychange', onVisibility);
            cleanupFns.push(() => document.removeEventListener('visibilitychange', onVisibility));
            isInitializing = false;
        });
    };

    if (isElementVisibleAndSized(inputColumn)) {
        start();
    } else {
        waitForVisibleSize(inputColumn, start);
    }
}

function clearPlaygroundOutput() {
    const output = document.getElementById('playground-output');
    if (output) output.innerHTML = '<div class="placeholder-text">Click "Run" to see the generated code.</div>';
    window.dispatchEvent(new Event('clear-output'));
}

function resetPlayground() {
    clearPlaygroundOutput();
    const templateSelect = document.getElementById('template-select');
    if (!templateSelect) return;

    const currentSlug = templateSelect.value;

    const url = `/playground/templates?template-select=${encodeURIComponent(currentSlug)}`;
    htmx.ajax('GET', url, {
        target: '#editor-source-container',
        swap: 'innerHTML'
    });
}

function setupEventListeners() {
    const playgroundContainer = document.querySelector('.playground-container');
    if (!playgroundContainer) return;
    const onClick = e => {
        const action = e.target.closest('[data-action]')?.dataset.action;
        if (!action) return;
        if (action === ACTIONS.RESET_ALL && !e.target.closest('.split-button-arrow')) resetPlayground();
        if (action === ACTIONS.CLEAR_OUTPUT) clearPlaygroundOutput();
    };
    playgroundContainer.addEventListener('click', onClick);
    cleanupFns.push(() => playgroundContainer.removeEventListener('click', onClick));
    const themeChangeHandler = e => {
        const monacoTheme = e?.detail?.monacoTheme;
        if (kreplicaEditor && monacoTheme) {
            monaco.editor.setTheme(monacoTheme);
            kreplicaEditor.layout();
        }
    };
    window.addEventListener('theme-changed', themeChangeHandler);
    cleanupFns.push(() => window.removeEventListener('theme-changed', themeChangeHandler));
    const editorSourceContainer = document.getElementById('editor-source-container');
    if (editorSourceContainer) {
        editorSourceContainer.addEventListener('htmx:afterSwap', updateEditorAfterSwap);
        cleanupFns.push(() => editorSourceContainer.removeEventListener('htmx:afterSwap', updateEditorAfterSwap));
    }
}

function getEditorInstance() {
    return kreplicaEditor;
}

function disposeEditor() {
    cleanupFns.forEach(fn => {
        try {
            fn();
        } catch {
        }
    });
    cleanupFns = [];
    if (kreplicaEditor) {
        const model = kreplicaEditor.getModel();
        if (model) model.dispose();
        kreplicaEditor.dispose();
        kreplicaEditor = null;
    }
    isInitializing = false;
}

function setEditorValue(value) {
    if (!kreplicaEditor) return;
    kreplicaEditor.setValue(value);
    const currentTextarea = getHiddenTextarea();
    if (currentTextarea) currentTextarea.value = value;
    requestAnimationFrame(resizeEditorToContent);
}

function setEditorModelFromSource(value) {
    if (!kreplicaEditor) return;
    const oldModel = kreplicaEditor.getModel();
    const newModel = monaco.editor.createModel(value, 'kotlin');
    kreplicaEditor.setModel(newModel);
    if (oldModel && oldModel !== newModel) oldModel.dispose();
    const currentTextarea = getHiddenTextarea();
    if (currentTextarea) currentTextarea.value = value;
    requestAnimationFrame(resizeEditorToContent);
}

function updateEditorAfterSwap() {
    const container = document.getElementById('editor-source-container');
    if (!container || !kreplicaEditor) return;
    const newSource = container.querySelector('textarea[name="source"]')?.value;
    if (typeof newSource === 'string') setEditorModelFromSource(newSource);
    const alpineComponent = document.querySelector('.playground-container')?.__x;
    if (alpineComponent) alpineComponent.data.isOutputReady = false;
    clearPlaygroundOutput();
}

const publicApi = {
    getEditorInstance,
    updateEditorAfterSwap,
};

export default {
    init() {
        initKReplicaPlayground();
        setupEventListeners();
        initOutputObserver();
        window.KREPLICA_PLAYGROUND = publicApi;
    },
    destroy() {
        disposeEditor();
        delete window.KREPLICA_PLAYGROUND;
    }
};
