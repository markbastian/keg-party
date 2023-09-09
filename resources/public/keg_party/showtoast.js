function showToast(targetId) {
    const toastElement = document.getElementById(targetId);
    const toast = new bootstrap.Toast(toastElement);
    toast.show();

    setTimeout(function () {
        toast.hide();
    }, 1000);
}
