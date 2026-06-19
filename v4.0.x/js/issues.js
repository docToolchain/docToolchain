const perPage = 100; // Maximum allowed by GitHub API

async function getAllIssues() {
    let allIssues = [];
    let page = 1;
    let hasMoreIssues = true;

    while (hasMoreIssues) {
        const url = `${issuesBaseUrl}?state=open&per_page=${perPage}&page=${page}`;
        const response = await fetch(url);
        const issues = await response.json();

        if (issues.length > 0) {
            allIssues = allIssues.concat(issues);
            page++;
        } else {
            hasMoreIssues = false;
        }
    }

    return allIssues;
}

function filterIssues(issues, term) {
    return issues.filter(issue => issue.body && issue.body.includes(term));
}

function displayIssues(issues) {
    const container = document.getElementById('issues-container');
    if (issues.length>0) {
        container.innerHTML = `<p>Found ${issues.length} issues:</p>`;

        issues.forEach(issue => {
            const issueElement = document.createElement('div');
            issueElement.className = 'issue';
            issueElement.innerHTML = `
                <img src="${issue.user.avatar_url}" alt="${issue.user.login}" class="user-avatar">
                <div class="issue-content">
                    <div class="issue-title">
                        <a href="${issue.html_url}" target="_blank">#${issue.number} - ${issue.title}</a>
                    </div>
                    <div class="issue-date">${new Date(issue.created_at).toLocaleString()}</div>
                </div>
            `;
            container.appendChild(issueElement);
        });
    } else {
        container.innerHTML = ``;
    }
}

document.addEventListener('DOMContentLoaded', async function () {
    const container = document.getElementById('issues-container');
    container.innerHTML = 'Loading...';

    try {
        const allIssues = await getAllIssues();
        const filteredIssues = filterIssues(allIssues, searchTerm);
        displayIssues(filteredIssues);
    } catch (error) {
        console.error('Error:', error);
        container.innerHTML = 'Error loading issues. Please check the console for details.';
    }
});