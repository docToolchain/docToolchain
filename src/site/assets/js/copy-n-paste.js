document.addEventListener('DOMContentLoaded', function() {
    // Get all divs with the 'copy' class
    const copyDivs = document.querySelectorAll('div.listingblock');

    copyDivs.forEach(div => {
        // Create a button
        const btn = document.createElement('i');
        btn.classList.add(['fa-light', 'fa-clipboard'])
        
        // Add click event to the button
        btn.addEventListener('click', async function() {
            try {
                await copyTableToClipboard(div);
                alert('Code copied to clipboard!');
            } catch (err) {
                console.error('Failed to copy code: ', err);
            }
        });

        // Append the button to the div
        div.appendChild(btn);
    });
});

async function copyTableToClipboard(listingblock) {
    const code = listingblock.querySelector('div.content').innerText;
    const blob = new Blob([code], { type: 'text/plain' });
    const data = [new ClipboardItem({ 'text/plain': blob })];
    await navigator.clipboard.write(data);
}
