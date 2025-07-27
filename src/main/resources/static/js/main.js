document.body.addEventListener('htmx:afterSwap', function (e) {
    Prism.highlightAllUnder(e.detail.elt);
    initScrollSpy();

    const requestPath = new URL(e.detail.xhr.responseURL).pathname;
    if (requestPath.startsWith('/guide/')) {
        setTimeout(() => scrollToActiveExample(), 0);
        const exampleShell = document.querySelector('#examples-main .examples-shell');
        applyHighlight(exampleShell);
    }

    const editorEl = document.getElementById('kreplica-editor');
    if (editorEl && !window.kreplicaEditor && typeof window.initKReplicaPlayground === 'function') {
        window.initKReplicaPlayground();
    }

    if (e.detail.target.id === 'editor-source-container') {
        if (window.kreplicaEditor) {
            const newSource = e.detail.target.querySelector('textarea[name="source"]').value;
            window.kreplicaEditor.setValue(newSource);
        }
        if (window.clearPlaygroundOutput) {
            window.clearPlaygroundOutput();
        }
    }
});

document.body.addEventListener('htmx:beforeSwap', function (evt) {
    if (evt.detail.target.id !== 'playground-output' && window.kreplicaEditor) {
        window.kreplicaEditor.dispose();
        window.kreplicaEditor = null;
    }
});

function applyHighlight(targetElement) {
    if (!targetElement) return;
    targetElement.addEventListener('animationend', () => {
        targetElement.classList.remove('flash-highlight');
    }, {once: true});
    targetElement.classList.add('flash-highlight');
}

function scrollToActiveExample() {
    const interactiveExamplesSection = document.getElementById('examples-main');
    if (interactiveExamplesSection) {
        const headerOffset = 85;
        const elementPosition = interactiveExamplesSection.getBoundingClientRect().top;
        const offsetPosition = elementPosition + window.scrollY - headerOffset;

        window.scrollTo({
            top: offsetPosition,
            behavior: "smooth"
        });
    }
}

let scrollSpyTimeoutId;
let isScrollSpyPaused = false;
let activeScrollListener = null;

function throttle(func, limit) {
    let inThrottle;
    return function () {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

function updateActiveNav(targetId) {
    const navContainers = document.querySelectorAll('#guide-sidebar-links, .fab-menu');
    navContainers.forEach(container => {
        container.querySelectorAll('.active').forEach(el => el.classList.remove('active'));
    });

    if (!targetId) return;

    const sidebarLink = document.querySelector(`#guide-sidebar-links a[href="#${targetId}"]`);
    if (sidebarLink) {
        sidebarLink.classList.add('active');
    }

    const fabMenu = document.querySelector('.fab-menu');
    if (!fabMenu) return;

    const fabLink = fabMenu.querySelector(`a[href="#${targetId}"]`);
    const fabButton = fabMenu.querySelector(`button[data-section-id="${targetId}"]`);

    if (fabLink) {
        const submenu = fabLink.closest('.fab-submenu');
        if (submenu && submenu.offsetParent !== null) {
            fabLink.classList.add('active');
        } else {
            const parentButton = submenu?.closest('li')?.querySelector('.fab-parent-item');
            if (parentButton) {
                parentButton.classList.add('active');
            } else {
                fabLink.classList.add('active');
            }
        }
    } else if (fabButton) {
        fabButton.classList.add('active');
    }

    const fabContainer = document.querySelector('.fab-container');
    const alpineData = fabContainer?.__x?.getUnwrappedData();
    const activeFabLink = fabMenu.querySelector('a.active');

    if (activeFabLink) {
        const submenu = activeFabLink.closest('.fab-submenu');
        if (submenu) {
            const parentButton = submenu.closest('li').querySelector('.fab-parent-item');
            if (parentButton && alpineData?.isOpen) {
                alpineData.openSection = parentButton.dataset.sectionId;
            }
        }
    }
}

function initScrollSpy() {
    if (activeScrollListener) {
        window.removeEventListener('scroll', activeScrollListener);
    }

    const linksContainer = document.getElementById('guide-sidebar-links');
    if (!linksContainer) return;

    let sections = Array.from(linksContainer.querySelectorAll('a[href^="#"]'))
        .map(link => document.getElementById(link.getAttribute('href').substring(1)))
        .filter(section => section !== null);

    if (sections.length === 0) return;

    sections.sort((a, b) => a.offsetTop - b.offsetTop);

    let lastActiveId = null;

    const handleScroll = () => {
        if (isScrollSpyPaused) return;

        let newActiveId = null;
        const scrollBottom = Math.ceil(window.innerHeight + window.scrollY);
        const docHeight = document.documentElement.scrollHeight;

        if (scrollBottom >= docHeight) {
            newActiveId = sections[sections.length - 1].id;
        } else {
            const scrollY = window.scrollY;
            const offset = 85;
            for (const section of sections) {
                if (section.offsetTop - offset <= scrollY) {
                    newActiveId = section.id;
                } else {
                    break;
                }
            }
        }

        if (newActiveId === null && sections.length > 0) {
            newActiveId = sections[0].id;
        }

        if (newActiveId !== lastActiveId) {
            lastActiveId = newActiveId;
            updateActiveNav(newActiveId);
            const scrollContainer = document.querySelector('.examples-sidebar');
            if (scrollContainer && scrollContainer.scrollHeight > scrollContainer.clientHeight) {
                const activeLinkInSidebar = scrollContainer.querySelector('a.active');
                if (activeLinkInSidebar) {
                    activeLinkInSidebar.scrollIntoView({
                        behavior: 'smooth',
                        block: 'nearest'
                    });
                }
            }
        }
    };

    activeScrollListener = throttle(handleScroll, 100);
    window.addEventListener('scroll', activeScrollListener);
    handleScroll();
}

function handleNavClick(event, sectionId) {
    event.preventDefault();
    isScrollSpyPaused = true;
    const targetSection = document.getElementById(sectionId);
    if (targetSection) {
        const headerOffset = 80;
        const elementPosition = targetSection.getBoundingClientRect().top;
        const offsetPosition = elementPosition + window.scrollY - headerOffset;

        window.scrollTo({
            top: offsetPosition,
            behavior: 'smooth'
        });

        applyHighlight(targetSection);
        updateActiveNav(sectionId);

        clearTimeout(scrollSpyTimeoutId);
        scrollSpyTimeoutId = setTimeout(() => {
            isScrollSpyPaused = false;
        }, 1000);
    }
}

window.handleFabNavClick = (event, sectionId) => {
    handleNavClick(event, sectionId);
};

window.handleFabLinkClick = (event, sectionId) => {
    handleNavClick(event, sectionId);
};

document.addEventListener('DOMContentLoaded', function () {
    const sidebarLinksContainer = document.getElementById('guide-sidebar-links');
    if (sidebarLinksContainer) {
        sidebarLinksContainer.addEventListener('click', function (e) {
            const anchorLink = e.target.closest('a[href^="#"]');
            if (anchorLink) {
                handleNavClick(e, anchorLink.getAttribute('href').substring(1));
            }
        });
    }

    const editorEl = document.getElementById('kreplica-editor');
    if (editorEl && !window.kreplicaEditor && typeof window.initKReplicaPlayground === 'function') {
        window.initKReplicaPlayground();
    }
    initScrollSpy();
    scrollToActiveExample();
});