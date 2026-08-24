import {getMonacoTheme} from '../utils/theme.js';
import {getCompletionProvider} from '../components/completion-provider.js';

let kreplicaEditor = null;
let cleanupFns = [];
let resizeObserver = null;
let isInitializing = false;
let languageModel = null;
let monacoLoaderPromise = null;
let visibilityFrameId = null;

const MOBILE_BREAKPOINT_PX = 992;
const MONACO_BASE_URL = 'https://unpkg.com/monaco-editor@0.52.2/min/vs';
const MONACO_LOADER_URL = `${MONACO_BASE_URL}/loader.js`;

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
        if (!isInitializing || !el.isConnected) {
            visibilityFrameId = null;
            return;
        }
        if (isElementVisibleAndSized(el)) {
            visibilityFrameId = null;
            ready();
        } else {
            visibilityFrameId = requestAnimationFrame(check);
        }
    };
    visibilityFrameId = requestAnimationFrame(check);
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

function loadMonacoLoader() {
    if (typeof window.require?.config === 'function') return Promise.resolve();
    if (monacoLoaderPromise) return monacoLoaderPromise;

    const existingLoader = document.querySelector(`script[src="${MONACO_LOADER_URL}"]`);
    const loaderPromise = new Promise((resolve, reject) => {
        if (existingLoader) {
            const timeoutId = setTimeout(() => reject(new Error('Timed out while loading Monaco editor')), 10000);
            existingLoader.addEventListener('load', () => {
                clearTimeout(timeoutId);
                resolve();
            }, {once: true});
            existingLoader.addEventListener('error', () => {
                clearTimeout(timeoutId);
                reject(new Error('Failed to load Monaco editor'));
            }, {once: true});
            return;
        }

        const script = document.createElement('script');
        script.src = MONACO_LOADER_URL;
        script.async = true;
        script.onload = resolve;
        script.onerror = () => reject(new Error('Failed to load Monaco editor.'));
        document.head.append(script);
    });
    monacoLoaderPromise = loaderPromise.catch(error => {
        monacoLoaderPromise = null;
        throw error;
    });
    return monacoLoaderPromise;
}

function initKReplicaPlayground() {
    if (kreplicaEditor || isInitializing) return;
    const editorNode = document.getElementById('kreplica-editor');
    const inputColumn = document.querySelector('.playground-input-column');
    if (!editorNode || !inputColumn) return;
    isInitializing = true;

    const start = async () => {
        try {
            await loadMonacoLoader();
        } catch (error) {
            console.error(error);
            isInitializing = false;
            return;
        }
        if (!isInitializing || !editorNode.isConnected) return;

        window.require.config({paths: {vs: MONACO_BASE_URL}});
        window.require(['vs/editor/editor.main'], async () => {
            if (!isInitializing || !editorNode.isConnected) return;
            try {
                const res = await fetch('/language-model.json');
                if (!res.ok) throw new Error(`Failed to load: ${res.statusText}`);
                languageModel = await res.json();
            } catch (e) {
                console.error('Failed to load KReplica language model:', e);
                languageModel = null;
            }
            if (!isInitializing || !editorNode.isConnected) return;

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

            const completionProvider = monaco.languages.registerCompletionItemProvider(
                'kotlin',
                getCompletionProvider(languageModel),
            );
            cleanupFns.push(() => completionProvider.dispose());

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
        void start();
    } else {
        waitForVisibleSize(inputColumn, () => void start());
        cleanupFns.push(() => {
            if (visibilityFrameId !== null) {
                cancelAnimationFrame(visibilityFrameId);
                visibilityFrameId = null;
            }
        });
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
        window.KREPLICA_PLAYGROUND = publicApi;
    },
    destroy() {
        disposeEditor();
        delete window.KREPLICA_PLAYGROUND;
    }
};
