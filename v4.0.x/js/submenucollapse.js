    function saveMenuState() {
        const openDetails = Array.from(document.querySelectorAll('details[open]')).map(el => el.id);
        localStorage.setItem('openDetails', JSON.stringify(openDetails));
    }

    function restoreMenuState() {
        const openDetails = JSON.parse(localStorage.getItem('openDetails')) || [];
        openDetails.forEach(id => {
            const details = document.getElementById(id);
            if (details) details.open = true;
        });
    }
    function openActiveItemPath() {
        const activeItem = document.querySelector('a.td-sidebar-link.active');
        if (activeItem) {
            let parent = activeItem.closest('details');
            while (parent) {
                parent.open = true;
                parent = parent.parentElement.closest('details');
            }
        }
    }
    document.querySelectorAll('details').forEach(details => {
        details.addEventListener('toggle', function() {
            saveMenuState();
        });
    });

    document.querySelectorAll('summary a').forEach(link => {
        link.addEventListener('click', function(e) {
            e.stopPropagation();
        });
    });

    // Zustand beim Laden der Seite wiederherstellen
    window.addEventListener('load', () => {
        restoreMenuState();
        openActiveItemPath();
    });
