/**
 * 拍后合成 - iOS Demo交互逻辑
 */

// View navigation
function showView(viewId) {
    // Hide all views
    document.querySelectorAll('.ios-view').forEach(view => {
        view.classList.remove('active');
    });

    // Show target view
    const targetView = document.getElementById(viewId);
    if (targetView) {
        targetView.classList.add('active');
    }

    // Update tab bar
    updateTabBar(viewId);
}

function updateTabBar(viewId) {
    document.querySelectorAll('.tab-item').forEach(tab => {
        tab.classList.remove('active');
    });

    // Map view to tab
    const viewToTab = {
        'homeView': 0,
        'createView': 1,
        'memberView': 2,
        'historyView': 3,
        'processingView': 1,
        'resultView': 3
    };

    const tabIndex = viewToTab[viewId];
    if (tabIndex !== undefined) {
        document.querySelectorAll('.tab-item')[tabIndex]?.classList.add('active');
    }
}

// Device selector
document.querySelectorAll('.device-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        document.querySelectorAll('.device-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');

        const device = this.dataset.device;
        const frame = document.getElementById('iphoneFrame');

        if (device === 'iphone-se') {
            frame.style.height = '667px';
            frame.style.borderRadius = '40px';
        } else {
            frame.style.height = '812px';
            frame.style.borderRadius = '50px';
        }
    });
});

// Segment buttons
document.querySelectorAll('.segment-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        this.parentElement.querySelectorAll('.segment-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
    });
});

// Copy functionality
document.querySelectorAll('.ios-copy-btn, .copy-single').forEach(btn => {
    btn.addEventListener('click', function() {
        // Visual feedback
        const originalText = this.textContent;
        this.textContent = '已复制';
        setTimeout(() => {
            this.textContent = originalText;
        }, 1500);
    });
});

// Tag click to copy
document.querySelectorAll('.ios-tag').forEach(tag => {
    tag.addEventListener('click', function() {
        // Visual feedback
        this.style.background = 'rgba(0, 122, 255, 0.1)';
        this.style.color = '#007AFF';
        setTimeout(() => {
            this.style.background = '';
            this.style.color = '';
        }, 1000);
    });
});

// Plan card selection
document.querySelectorAll('.plan-card').forEach(card => {
    card.addEventListener('click', function() {
        document.querySelectorAll('.plan-card').forEach(c => {
            c.style.borderColor = 'transparent';
        });
        this.style.borderColor = '#667eea';
    });
});

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    // Default to home view
    showView('homeView');

    // Add touch feedback to interactive elements
    document.querySelectorAll('.action-card, .work-card, .history-item, .ios-btn-primary, .ios-btn-secondary, .tab-item').forEach(el => {
        el.addEventListener('touchstart', function() {
            this.style.opacity = '0.7';
        });
        el.addEventListener('touchend', function() {
            this.style.opacity = '1';
        });
    });
});

// Simulate processing animation
let processingInterval;
function startProcessingAnimation() {
    const percent = document.querySelector('.progress-percent');
    const statusDetail = document.querySelector('.status-detail');
    let progress = 0;

    const stages = [
        '镜头分析中...',
        '脚本生成中...',
        'AI配音合成中...',
        '视频渲染中...'
    ];

    processingInterval = setInterval(() => {
        progress += 1;
        percent.textContent = progress + '%';

        const stageIndex = Math.floor(progress / 25);
        if (stageIndex < stages.length) {
            statusDetail.textContent = stages[stageIndex];
        }

        // Update step indicators
        const steps = document.querySelectorAll('#processingView .p-step');
        steps.forEach((step, index) => {
            step.classList.remove('active', 'completed');
            if (index < stageIndex) {
                step.classList.add('completed');
            } else if (index === stageIndex) {
                step.classList.add('active');
            }
        });

        if (progress >= 100) {
            clearInterval(processingInterval);
            setTimeout(() => {
                showView('resultView');
            }, 500);
        }
    }, 50);
}

// Handle window visibility change to pause animations
document.addEventListener('visibilitychange', () => {
    if (document.hidden && processingInterval) {
        clearInterval(processingInterval);
    }
});
