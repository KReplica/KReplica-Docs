import {initScrollSpy} from '../components/scroll-spy.js';
import {initGuideNavigation} from '../components/guide-navigation.js';

let scrollSpyTimeoutId;

function applyHighlight(targetElement) {
    if (!targetElement) return;
    targetElement.addEventListener('animationend', () => {
        targetElement.classList.remove('flash-highlight');
    }, {once: true});
    targetElement.classList.add('flash-highlight');
}

function handleNavClick(event, sectionId) {
    event.preventDefault();
    window.isScrollSpyPaused = true;
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
        const customEvent = new CustomEvent('section-active', {detail: {sectionId}});
        document.body.dispatchEvent(customEvent);

        clearTimeout(scrollSpyTimeoutId);
        scrollSpyTimeoutId = setTimeout(() => {
            window.isScrollSpyPaused = false;
        }, 1000);
    }
}

export function init() {
    window.isScrollSpyPaused = false;
    initScrollSpy();
    initGuideNavigation();

    window.handleFabNavClick = (event, sectionId) => {
        handleNavClick(event, sectionId);
    };

    window.handleFabLinkClick = (event, sectionId) => {
        handleNavClick(event, sectionId);
    };

    const sidebarLinksContainer = document.querySelector('[data-js-id="guide-sidebar-links"]');
    if (sidebarLinksContainer) {
        sidebarLinksContainer.addEventListener('click', function (e) {
            const anchorLink = e.target.closest('a[href^="#"]');
            if (anchorLink) {
                handleNavClick(e, anchorLink.getAttribute('href').substring(1));
            }
        });
    }
}