document.addEventListener('DOMContentLoaded', function() {
    // Get all divs with the 'copy' class
    const copyDivs = document.querySelectorAll('div.listingblock');

    copyDivs.forEach(div => {
        // Create a button
        const span = document.createElement('span')
        span.classList.add('icon')

        const icon = document.createElement('i');
        icon.classList.add('fa')
        icon.classList.add('fa-clipboard')
        span.appendChild(icon)

        // Add click event to the button
        icon.addEventListener('click', async function() {
            try {
                await copyTableToClipboard(div);
                alert('Code copied to clipboard!');
            } catch (err) {
                console.error('Failed to copy code: ', err);
            }
        });

        // Append the button to the div
        div.appendChild(span);
    });
});

async function copyTableToClipboard(listingblock) {
    const code = listingblock.querySelector('div.content').innerText+"\n";
    const blob = new Blob([code], { type: 'text/plain' });
    const data = [new ClipboardItem({ 'text/plain': blob })];
    await navigator.clipboard.write(data);
}
