function showToast(targetId) {
    const toastElement = document.getElementById(targetId);
    const toast = new bootstrap.Toast(toastElement);
    toast.show();

    setTimeout(function () {
        toast.hide();
    }, 1000);
}

var checkSamePassword = function() {
    if (document.getElementById('enter-password').value ===
        document.getElementById('confirm-password').value) {
        document.getElementById('message').style.color = 'green';
        document.getElementById('message').innerHTML = 'passwords match';
        document.getElementById('password-submit-button').disabled = false;
    } else {
        document.getElementById('message').style.color = 'red';
        document.getElementById('message').innerHTML = 'passwords do not match';
        document.getElementById('password-submit-button').disabled = true;
    }
}
