import {destroyScrollSpy, initScrollSpy, pauseScrollSpy, resumeScrollSpy} from "../components/scroll-spy.js";
import {destroyGuideNavigation, initGuideNavigation} from "../components/guide-navigation.js";

const HEADER_OFFSET_PX = 80;

function applyHighlight(targetElement) {
    if (!targetElement) return;
    targetElement.addEventListener("animationend", () =>
            targetElement.classList.remove("flash-highlight"),
        {once: true}
    );
    targetElement.classList.add("flash-highlight");
}

function smoothScrollToSection(section) {
    pauseScrollSpy();
    const offsetPosition = section.getBoundingClientRect().top + window.scrollY - HEADER_OFFSET_PX;
    window.scrollTo({top: offsetPosition, behavior: "smooth"});
    applyHighlight(section);
    document.body.dispatchEvent(new CustomEvent("section-active", {detail: {sectionId: section.id}}));
    window.addEventListener('scrollend', resumeScrollSpy, {once: true});
}

function handleNavClick(event, sectionId) {
    event.preventDefault();
    const targetSection = document.getElementById(sectionId);
    if (targetSection) smoothScrollToSection(targetSection);
    else resumeScrollSpy();
}

function bindSidebarNavClicks() {
    const sidebar = document.querySelector('[data-js-id="guide-sidebar-links"]');
    if (!sidebar) return;
    sidebar.addEventListener("click", e => {
        const anchor = e.target.closest('a[href^="#"]');
        if (anchor) handleNavClick(e, anchor.getAttribute("href").slice(1));
    });
}

function bindFabNavClicks() {
    const fab = document.querySelector('[data-js-id="fab-container"]');
    if (!fab) return;
    fab.addEventListener("click", e => {
        const anchor = e.target.closest('a[href^="#"]');
        if (!anchor) return;
        handleNavClick(e, anchor.getAttribute("href").slice(1));
    });
}

function bindContentNavClicks() {
    const contentArea = document.querySelector('.guide-main-content');
    if (!contentArea) return;
    contentArea.addEventListener('click', e => {
        const anchor = e.target.closest('a[href^="#"]');
        if (anchor) {
            const href = anchor.getAttribute('href');
            if (href.length > 1) {
                handleNavClick(e, href.slice(1));
            }
        }
    });
}

export default {
    init() {
        initGuideNavigation();
        initScrollSpy();
        bindSidebarNavClicks();
        bindFabNavClicks();
        bindContentNavClicks();
    },
    destroy() {
        destroyScrollSpy();
        destroyGuideNavigation();
    }
};