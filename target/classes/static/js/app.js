document.addEventListener('DOMContentLoaded', () => {
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const fileInfo = document.getElementById('fileInfo');
    const fileCount = document.getElementById('fileCount');
    const clearBtn = document.getElementById('clearBtn');
    const fileNameDisplay = document.querySelector('.file-name');
    const analyzeBtn = document.getElementById('analyzeBtn');
    const loader = document.getElementById('loader');
    const resultsSection = document.getElementById('resultsSection');
    const tenantIdInput = document.getElementById('tenantId');
    
    // Result Elements
    const severityBadge = document.getElementById('severityBadge');
    const logSummary = document.getElementById('logSummary');
    const errorList = document.getElementById('errorList');
    const suggestedSolution = document.getElementById('suggestedSolution');

    // Jira Elements
    const jiraModal = document.getElementById('jiraModal');
    const showJiraBtn = document.getElementById('showJiraBtn');
    const closeModal = document.getElementById('closeModal');
    const cancelJira = document.getElementById('cancelJira');
    const confirmJira = document.getElementById('confirmJira');
    const jiraSummary = document.getElementById('jiraSummary');
    const jiraDescription = document.getElementById('jiraDescription');
    const jiraPriority = document.getElementById('jiraPriority');

    let selectedFiles = [];
    let currentAnalysis = null;

    // --- Drag & Drop ---
    dropZone.addEventListener('click', () => fileInput.click());

    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('active');
    });

    ['dragleave', 'drop'].forEach(event => {
        dropZone.addEventListener(event, () => dropZone.classList.remove('active'));
    });

    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        if (e.dataTransfer.files.length) handleFilesSelect(e.dataTransfer.files);
    });

    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length) handleFilesSelect(e.target.files);
    });

    function handleFilesSelect(files) {
        selectedFiles = Array.from(files);
        if (selectedFiles.length === 1) {
            fileNameDisplay.textContent = selectedFiles[0].name;
            fileCount.textContent = 'Ready for analysis';
        } else {
            fileNameDisplay.textContent = `${selectedFiles.length} Files Selected`;
            fileCount.textContent = selectedFiles.map(f => f.name).join(', ');
        }
        
        fileInfo.classList.remove('hidden');
        dropZone.classList.add('hidden');
        resultsSection.classList.add('hidden');
    }

    const clearResultsBtn = document.getElementById('clearResultsBtn');
    
    // Reset function
    function resetUI() {
        selectedFiles = [];
        fileInput.value = '';
        fileInfo.classList.add('hidden');
        dropZone.classList.remove('hidden');
        resultsSection.classList.add('hidden');
    }

    // --- Events ---
    clearBtn.addEventListener('click', resetUI);
    clearResultsBtn.addEventListener('click', resetUI);

    // --- AI Analysis ---
    analyzeBtn.addEventListener('click', async () => {
        if (!selectedFiles.length) return;

        const tenantId = tenantIdInput.value || 'DEFAULT_TENANT';
        const formData = new FormData();
        
        // We'll send the files to the backend
        // Note: Our current backend endpoint /analyze takes a single file
        // To keep it simple, we will send only the first one OR 
        // we can update the backend to take multiple files.
        // Let's update the backend to take List<MultipartFile> for better UX.
        
        selectedFiles.forEach(file => {
            formData.append('files', file);
        });

        loader.classList.remove('hidden');
        fileInfo.classList.add('hidden');
        resultsSection.classList.add('hidden');

        try {
            const response = await fetch('/api/v1/manual/analyze', {
                method: 'POST',
                headers: { 'X-Tenant-ID': tenantId },
                body: formData
            });

            if (!response.ok) throw new Error('Analysis failed');

            currentAnalysis = await response.json();
            displayResults(currentAnalysis);

        } catch (error) {
            showToast('Error: ' + error.message, 'error');
            fileInfo.classList.remove('hidden');
        } finally {
            loader.classList.add('hidden');
        }
    });

    function displayResults(data) {
        resultsSection.classList.remove('hidden');
        
        // Severity
        severityBadge.textContent = data.severity || 'UNKNOWN';
        severityBadge.className = 'severity-badge ' + `severity-${(data.severity || 'medium').toLowerCase()}`;
        
        // Summary
        logSummary.textContent = data.logSummary;
        
        // Errors
        errorList.innerHTML = '';
        if (data.identifiedErrors && data.identifiedErrors.length) {
            data.identifiedErrors.forEach(err => {
                const li = document.createElement('li');
                li.textContent = err;
                errorList.appendChild(li);
            });
        } else {
            errorList.innerHTML = '<li>No specific errors parsed. Check raw details.</li>';
        }
        
        // Solution
        suggestedSolution.textContent = data.suggestedSolution;

        // Populate Jira fields
        jiraSummary.value = `[Manual] ${data.logSummary.substring(0, 50)}...`;
        jiraDescription.value = `AI Analysis Result:\n\nSummary: ${data.logSummary}\n\nErrors: ${data.identifiedErrors.join(', ')}\n\nSuggested Solution: ${data.suggestedSolution}`;
        jiraPriority.value = mapSeverityToPriority(data.severity);
    }

    function mapSeverityToPriority(sev) {
        const s = (sev || '').toUpperCase();
        if (s === 'CRITICAL') return 'Highest';
        if (s === 'HIGH') return 'High';
        if (s === 'LOW') return 'Low';
        return 'Medium';
    }

    // --- Jira Integration ---
    showJiraBtn.addEventListener('click', () => jiraModal.classList.remove('hidden'));
    
    [closeModal, cancelJira].forEach(btn => {
        btn.addEventListener('click', () => jiraModal.classList.add('hidden'));
    });

    confirmJira.addEventListener('click', async () => {
        const payload = {
            summary: jiraSummary.value,
            description: jiraDescription.value,
            priority: jiraPriority.value
        };

        confirmJira.disabled = true;
        confirmJira.textContent = 'Creating...';

        try {
            const response = await fetch('/api/v1/manual/jira/create', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'X-Tenant-ID': tenantIdInput.value || 'DEFAULT_TENANT'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) throw new Error('Jira creation failed');

            showToast('Jira Ticket Created Successfully!', 'success');
            jiraModal.classList.add('hidden');

        } catch (error) {
            showToast('Error: ' + error.message, 'error');
        } finally {
            confirmJira.disabled = false;
            confirmJira.textContent = 'Create Ticket';
        }
    });

    // --- Toast Notification ---
    function showToast(message, type = 'success') {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.style.borderBottomColor = type === 'error' ? 'var(--error)' : 'var(--accent)';
        toast.classList.remove('hidden');
        
        setTimeout(() => {
            toast.classList.add('hidden');
        }, 4000);
    }
});
