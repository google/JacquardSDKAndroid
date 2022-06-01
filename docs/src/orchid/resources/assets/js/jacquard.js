const JacquardDoc = {
    toggleSearch: () => {
        const searchPanel = document.getElementById("search");
        if (searchPanel.className == "search-active") {
            searchPanel.className = "";
            document.getElementById("search-progress").style.display = "none";
            document.getElementById("search-results").style.display = "none";
        } else {
            searchPanel.className = "search-active";
            document.getElementById("search_input").focus();
        }
    }
}