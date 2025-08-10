let isInitialized = false;

function updateSidebar(sectionId) {
    const sidebarLinks = document.querySelector('[data-js-id="guide-sidebar-links"]');
    if (!sidebarLinks) return;
    sidebarLinks.querySelectorAll('.active').forEach(el => el.classList.remove('active'));
    const newActiveLink = sidebarLinks.querySelector(`a[href="#${sectionId}"]`);
    if (newActiveLink) {
        newActiveLink.classList.add('active');
        const sidebarContainer = sidebarLinks.closest('.examples-sidebar');
        if (sidebarContainer && sidebarContainer.scrollHeight > sidebarContainer.clientHeight) {
            newActiveLink.scrollIntoView({behavior: 'smooth', block: 'nearest'});
        }
    }
}

function updateFabMenu(sectionId) {
    const fabMenu = document.querySelector('[data-js-id="fab-menu"]');
    if (!fabMenu) return;
    fabMenu.querySelectorAll('.active').forEach(el => el.classList.remove('active'));
    const fabLink = fabMenu.querySelector(`a[href="#${sectionId}"]`);
    const fabButton = fabMenu.querySelector(`button[data-section-id="${sectionId}"]`);
    if (fabLink) {
        const submenu = fabLink.closest('.fab-submenu');
        if (submenu && submenu.offsetParent !== null) {
            fabLink.classList.add('active');
        } else {
            const parentButton = submenu?.closest('li')?.querySelector('.fab-parent-item');
            if (parentButton) parentButton.classList.add('active');
            else fabLink.classList.add('active');
        }
    } else if (fabButton) {
        fabButton.classList.add('active');
    }
}

const handleSectionActive = (event) => {
    const {sectionId} = event.detail || {};
    if (sectionId) {
        updateSidebar(sectionId);
        updateFabMenu(sectionId);
    }
};

export function initGuideNavigation() {
    if (isInitialized) return;
    document.body.addEventListener('section-active', handleSectionActive);
    isInitialized = true;
}

export function destroyGuideNavigation() {
    if (!isInitialized) return;
    document.body.removeEventListener('section-active', handleSectionActive);
    isInitialized = false;
}