function showToast(targetId) {
    const toastElement = document.getElementById(targetId);
    const toast = new bootstrap.Toast(toastElement);
    toast.show();

    setTimeout(function () {
        toast.hide();
    }, 1000);
}

var checkSamePassword = function () {
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

var toggleSidebar = function () {
    expandedWidth = '250px';
    collapsedWidth = '25px';
    sidebarStyle = document.getElementById('tap-sidebar').style;
    taplogStyle = document.getElementById('tap-log').style;
    toggleButton = document.getElementById('toggle-sidebar');
    if (sidebarStyle.width !== collapsedWidth) {
        sidebarStyle.width = collapsedWidth;
        taplogStyle.marginLeft = collapsedWidth;
        toggleButton.innerHTML = '<i class="fa-solid fa-angles-right"></i>'
    } else {
        sidebarStyle.width = expandedWidth;
        taplogStyle.marginLeft = expandedWidth;
        toggleButton.innerHTML = '<i class="fa-solid fa-angles-left"></i>'
    }
}
