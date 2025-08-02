import {destroyScrollSpy, initScrollSpy, pauseScrollSpy, resumeScrollSpy} from "../components/scroll-spy.js";
import {destroyGuideNavigation, initGuideNavigation} from "../components/guide-navigation.js";

const HEADER_OFFSET_PX = 80;
let scrollSpyTimeoutId;

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
    clearTimeout(scrollSpyTimeoutId);
    scrollSpyTimeoutId = setTimeout(resumeScrollSpy, 1000);
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
        fab.dispatchEvent(new CustomEvent('close'));
        handleNavClick(e, anchor.getAttribute("href").slice(1));
    });
}

export default {
    init() {
        initGuideNavigation();
        initScrollSpy();
        bindSidebarNavClicks();
        bindFabNavClicks();
    },
    destroy() {
        destroyScrollSpy();
        destroyGuideNavigation();
    }
};