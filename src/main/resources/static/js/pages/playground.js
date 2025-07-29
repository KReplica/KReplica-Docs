let kreplicaEditor = null;
let lastContentHeight = 0;

function getHiddenTextarea() {
    return document.querySelector('textarea[name="source"]');
}

function isMobile() {
    return window.innerWidth <= 991;
}

function computeAvailableEditorMaxHeight() {
    const scroller = document.querySelector('.page-playground .main-content');
    const actions = document.querySelector('.playground-actions-mobile');
    const editorNode = document.getElementById('kreplica-editor');
    if (!scroller || !actions || !editorNode) return null;
    const scrollerRect = scroller.getBoundingClientRect();
    const editorRect = editorNode.getBoundingClientRect();
    const reserved = actions.offsetHeight || 0;
    const available = Math.floor(scrollerRect.bottom - editorRect.top - reserved - 16);
    return available > 0 ? available : null;
}

function fitToContent(heightHint) {
    if (!kreplicaEditor) return;
    const editorNode = document.getElementById('kreplica-editor');
    const measured = typeof heightHint === 'number' ? heightHint : kreplicaEditor.getContentHeight();
    lastContentHeight = measured > 0 ? measured : lastContentHeight;
    let targetHeight = lastContentHeight;
    if (isMobile()) {
        const maxH = computeAvailableEditorMaxHeight();
        if (typeof maxH === 'number') {
            targetHeight = Math.min(targetHeight, maxH);
        }
    }
    const width = editorNode.clientWidth || editorNode.offsetWidth || 0;
    editorNode.style.height = targetHeight + 'px';
    if (width > 0) {
        kreplicaEditor.layout({width, height: targetHeight});
    } else {
        kreplicaEditor.layout();
    }
}

function scheduleFit() {
    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            fitToContent();
        });
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

                kreplicaEditor = monaco.editor.create(
                    editorNode,
                    {
                        value: initialCode,
                        language: 'kotlin',
                        automaticLayout: true,
                        theme: 'vs-dark',
                        minimap: {enabled: false},
                        folding: true,
                        scrollBeyondLastLine: false,
                        lineHeight: 20
                    }
                );

                monaco.languages.registerCompletionItemProvider('kotlin', {
                    triggerCharacters: ['@', '.'],
                    provideCompletionItems() {
                        return {suggestions: KREPLICA_COMPLETIONS};
                    }
                });

                kreplicaEditor.onDidContentSizeChange((e) => {
                    fitToContent(e.contentHeight);
                });

                kreplicaEditor.onDidChangeModel(() => {
                    scheduleFit();
                });

                kreplicaEditor.onDidChangeModelContent(() => {
                    const currentTextarea = getHiddenTextarea();
                    if (currentTextarea) {
                        currentTextarea.value = kreplicaEditor.getValue();
                    }
                });

                window.addEventListener('resize', scheduleFit, {passive: true});

                scheduleFit();
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

    playgroundContainer.addEventListener('click', (e) => {
        const actionTarget = e.target.closest('[data-action]');
        if (!actionTarget) return;

        const action = actionTarget.dataset.action;

        switch (action) {
            case 'run':
                break;
            case 'reset-all':
                if (!e.target.closest('.split-button-arrow')) {
                    resetPlayground();
                }
                break;
            case 'clear-output':
                clearPlaygroundOutput();
                break;
        }
    });

    window.addEventListener('resize', scheduleFit, {passive: true});
}

export function init() {
    initKReplicaPlayground();
    setupEventListeners();
}

export function getEditorInstance() {
    return kreplicaEditor;
}

export function disposeEditor() {
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
        scheduleFit();
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
    scheduleFit();
}

export {clearPlaygroundOutput};