import {initScrollSpy, pauseScrollSpy, resumeScrollSpy} from '../components/scroll-spy.js';
import {initGuideNavigation} from '../components/guide-navigation.js';

let scrollSpyTimeoutId;

function applyHighlight(targetElement) {
    if (!targetElement) return;
    targetElement.addEventListener('animationend', () => targetElement.classList.remove('flash-highlight'), {once: true});
    targetElement.classList.add('flash-highlight');
}

function handleNavClick(event, sectionId) {
    event.preventDefault();
    pauseScrollSpy();
    const targetSection = document.getElementById(sectionId);
    if (targetSection) {
        const headerOffset = 80;
        const offsetPosition = targetSection.getBoundingClientRect().top + window.scrollY - headerOffset;
        window.scrollTo({top: offsetPosition, behavior: 'smooth'});
        applyHighlight(targetSection);
        document.body.dispatchEvent(new CustomEvent('section-active', {detail: {sectionId}}));
        clearTimeout(scrollSpyTimeoutId);
        scrollSpyTimeoutId = setTimeout(resumeScrollSpy, 1000);
    } else {
        resumeScrollSpy();
    }
}

function setupEventListeners() {
    const sidebar = document.querySelector('[data-js-id="guide-sidebar-links"]');
    if (sidebar) {
        sidebar.addEventListener('click', (e) => {
            const anchor = e.target.closest('a[href^="#"]');
            if (anchor) handleNavClick(e, anchor.getAttribute('href').slice(1));
        });
    }
    const fab = document.querySelector('[data-js-id="fab-container"]');
    if (fab) {
        fab.addEventListener('click', (e) => {
            const anchor = e.target.closest('a[href^="#"]');
            if (anchor) {
                const fabContainer = e.target.closest('[x-data]');
                if (fabContainer?.__x) fabContainer.__x.data.isOpen = false;
                handleNavClick(e, anchor.getAttribute('href').slice(1));
            }
        });
    }
}

export function init() {
    initGuideNavigation();
    initScrollSpy();
    setupEventListeners();
}