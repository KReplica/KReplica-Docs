let kreplicaEditor = null;
let cleanupFns = [];
let resizeObserver = null;
let outputObserver = null;

const MOBILE_BREAKPOINT_PX = 992;

const ACTIONS = {
    RUN: 'run',
    RESET_ALL: 'reset-all',
    CLEAR_OUTPUT: 'clear-output'
};

function isMobile() {
    return window.innerWidth < MOBILE_BREAKPOINT_PX;
}

function getHiddenTextarea() {
    return document.querySelector('textarea[name="source"]');
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
    const width = editorNode.clientWidth || editorNode.parentElement.clientWidth || 0;
    kreplicaEditor.layout({width, height});
}

function initOutputObserver() {
    if (outputObserver) {
        outputObserver.disconnect();
    }

    const outputNode = document.getElementById('playground-output');
    if (!outputNode) return;

    const observerCallback = (mutationsList) => {
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
    if (kreplicaEditor) {
        return;
    }

    require.config({paths: {vs: 'https://unpkg.com/monaco-editor@0.52.2/min/vs'}});
    require(['vs/editor/editor.main'], function () {
        fetch('/api/completions')
            .then(response => response.json())
            .then(completionsData => {
                const KREPLICA_COMPLETIONS = completionsData.map(item => ({
                    label: item.label,
                    kind: monaco.languages.CompletionItemKind[item.kind] || monaco.languages.CompletionItemKind.Text,
                    insertText: item.insertText,
                    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
                }));

                const hiddenTextareaEl = getHiddenTextarea();
                const initialCode = hiddenTextareaEl ? hiddenTextareaEl.value : '';
                const editorNode = document.getElementById('kreplica-editor');
                const currentSiteTheme = document.documentElement.getAttribute('data-theme') || 'light';
                const initialMonacoTheme = currentSiteTheme === 'dark' ? 'vs-dark' : 'vs';

                kreplicaEditor = monaco.editor.create(editorNode, {
                    value: initialCode,
                    language: 'kotlin',
                    automaticLayout: false,
                    theme: initialMonacoTheme,
                    minimap: {enabled: false},
                    folding: true,
                    scrollBeyondLastLine: false,
                    scrollbar: {vertical: isMobile() ? 'auto' : 'hidden'},
                    lineHeight: 20
                });

                monaco.languages.registerCompletionItemProvider('kotlin', {
                    triggerCharacters: ['@', '.'],
                    provideCompletionItems() {
                        return {suggestions: KREPLICA_COMPLETIONS};
                    }
                });

                kreplicaEditor.onDidChangeModelContent(() => {
                    const currentTextarea = getHiddenTextarea();
                    if (currentTextarea) {
                        currentTextarea.value = kreplicaEditor.getValue();
                    }
                });

                kreplicaEditor.onDidContentSizeChange(() => {
                    resizeEditorToContent();
                });

                resizeEditorToContent();

                resizeObserver = new ResizeObserver(() => {
                    resizeEditorToContent();
                });
                const observeTarget = editorNode.parentElement || editorNode;
                resizeObserver.observe(observeTarget);
                cleanupFns.push(() => {
                    if (resizeObserver) {
                        try {
                            resizeObserver.disconnect();
                        } catch (_) {
                        }
                        resizeObserver = null;
                    }
                });

                const onVisibility = () => {
                    if (document.visibilityState === 'visible') {
                        resizeEditorToContent();
                    }
                };
                document.addEventListener('visibilitychange', onVisibility);
                cleanupFns.push(() => document.removeEventListener('visibilitychange', onVisibility));
            });
    });
}

function clearPlaygroundOutput() {
    const output = document.getElementById('playground-output');
    if (output) {
        output.innerHTML = '<div class="placeholder-text">Click "Run" to see the generated code.</div>';
    }
    window.dispatchEvent(new Event('clear-output'));
}

function resetPlayground() {
    clearPlaygroundOutput();
    const container = document.getElementById('editor-source-container');
    if (!container) return;
    const slugEl = container.querySelector('[data-template-slug]');
    const slug = slugEl ? slugEl.dataset.templateSlug : null;
    if (!slug) return;
    const url = `/playground/templates?template-select=${slug}`;
    htmx.ajax('GET', url, {target: '#editor-source-container', swap: 'innerHTML'});
}

function setupEventListeners() {
    const playgroundContainer = document.querySelector('.playground-container');
    if (!playgroundContainer) return;

    const onClick = (e) => {
        const actionTarget = e.target.closest('[data-action]');
        if (!actionTarget) return;

        const action = actionTarget.dataset.action;

        switch (action) {
            case ACTIONS.RUN:
                break;
            case ACTIONS.RESET_ALL:
                if (!e.target.closest('.split-button-arrow')) {
                    resetPlayground();
                }
                break;
            case ACTIONS.CLEAR_OUTPUT:
                clearPlaygroundOutput();
                break;
        }
    };

    playgroundContainer.addEventListener('click', onClick);
    cleanupFns.push(() => playgroundContainer.removeEventListener('click', onClick));

    const themeChangeHandler = (e) => {
        if (kreplicaEditor && e.detail.monacoTheme) {
            monaco.editor.setTheme(e.detail.monacoTheme);
        }
    };
    window.addEventListener('theme-changed', themeChangeHandler);
    cleanupFns.push(() => window.removeEventListener('theme-changed', themeChangeHandler));
}

export function init() {
    initKReplicaPlayground();
    setupEventListeners();
    initOutputObserver();
}

export function getEditorInstance() {
    return kreplicaEditor;
}

export function disposeEditor() {
    cleanupFns.forEach(fn => {
        try {
            fn();
        } catch (_) {
        }
    });
    cleanupFns = [];
    if (kreplicaEditor) {
        const model = kreplicaEditor.getModel();
        if (model) {
            model.dispose();
        }
        kreplicaEditor.dispose();
        kreplicaEditor = null;
    }
}

export function setEditorValue(value) {
    if (kreplicaEditor) {
        kreplicaEditor.setValue(value);
        const currentTextarea = getHiddenTextarea();
        if (currentTextarea) {
            currentTextarea.value = value;
        }
        requestAnimationFrame(() => {
            resizeEditorToContent();
        });
    }
}

export function setEditorModelFromSource(value) {
    if (!kreplicaEditor) return;
    const oldModel = kreplicaEditor.getModel();
    const newModel = monaco.editor.createModel(value, 'kotlin');
    kreplicaEditor.setModel(newModel);
    if (oldModel && oldModel !== newModel) {
        oldModel.dispose();
    }
    const currentTextarea = getHiddenTextarea();
    if (currentTextarea) {
        currentTextarea.value = value;
    }
    requestAnimationFrame(() => {
        resizeEditorToContent();
    });
}

export function updateEditorAfterSwap() {
    const container = document.getElementById('editor-source-container');
    if (!container || !kreplicaEditor) return;

    const newSource = container.querySelector('textarea[name="source"]')?.value;
    if (typeof newSource === 'string') {
        setEditorModelFromSource(newSource);
    }

    const alpineComponent = document.querySelector('.playground-container')?.__x;
    if (alpineComponent) {
        alpineComponent.data.isOutputReady = false;
    }
    clearPlaygroundOutput();
}

export {clearPlaygroundOutput};