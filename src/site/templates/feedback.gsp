<style>
.feedback--answer {
    display: inline-block;
}

.feedback--answer-no {
    margin-left: 1em;
}

.feedback--response {
    display: none;
    margin-top: 1em;
}

.feedback--response__visible {
    display: block;
}
</style>
<%
    //let's build some urls.
    //what's the correct source file name with path?
    sourceFileName = content?.uri?.replaceAll("[.]html", (content.file =~ /[.][^.]+$/)[0])
    def subject = java.net.URLEncoder.encode("Docs: Feedback for '${content?.title}'", "UTF-8")
%>
<div class="d-print-none">
    <h2 class="feedback--title">Feedback</h2>
    <p class="feedback--question">Was this page helpful?</p>
    <button class="feedback--answer feedback--answer-yes">Yes</button>
    <button class="feedback--answer feedback--answer-no">No</button>
    <p class="feedback--response feedback--response-yes">
        Glad to hear it! Please <a href="${config.site_issueUrl}?title=${subject}%20%F0%9F%91%8D&body=%0A%0A%5BEnter%20feedback%20here%5D%0A%0A%0A---%0A%23page:${sourceFileName}">tell us
    how we can improve</a>.
    </p>
    <p class="feedback--response feedback--response-no">
        Sorry to hear that. Please <a href="${config.site_issueUrl}?title=${subject}%20%F0%9F%91%8E&body=%0A%0A%5BEnter%20feedback%20here%5D%0A%0A%0A---%0A%23page:${sourceFileName}">tell
    us how we can improve</a>.
    </p>
</div>
<script>
    const yesButton = document.querySelector('.feedback--answer-yes');
    const noButton = document.querySelector('.feedback--answer-no');
    const yesResponse = document.querySelector('.feedback--response-yes');
    const noResponse = document.querySelector('.feedback--response-no');
    const disableButtons = () => {
        yesButton.disabled = true;
        noButton.disabled = true;
    };
    const sendFeedback = (value) => {
        if (typeof ga !== 'function') return;
        const args = {
            command: 'send',
            hitType: 'event',
            category: 'Helpful',
            action: 'click',
            label: window.location.pathname,
            value: value
        };
        ga(args.command, args.hitType, args.category, args.action, args.label, args.value);
    };
    yesButton.addEventListener('click', () => {
        yesResponse.classList.add('feedback--response__visible');
        disableButtons();
        sendFeedback(1);
    });
    noButton.addEventListener('click', () => {
        noResponse.classList.add('feedback--response__visible');
        disableButtons();
        sendFeedback(0);
    });
</script>
