#!/usr/bin/env python3
"""
Script to apply the modern sidebar to all UWS HTML pages.
This script will add the sidebar navigation to all service pages.
"""

import os
import re
from pathlib import Path

# Sidebar HTML template
SIDEBAR_HTML = '''    <!-- Sidebar -->
    <div class="sidebar" id="sidebar">
        <div class="sidebar-header">
            <a href="/dashboard.html" class="sidebar-brand">🦄 Unicorn</a>
        </div>
        <nav class="sidebar-nav">
            <div class="nav-item">
                <a href="/dashboard.html" class="nav-link">
                    <i class="fas fa-tachometer-alt"></i>
                    Dashboard
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-billing.html" class="nav-link">
                    <i class="fas fa-credit-card"></i>
                    UWS-Billing
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-monitoring.html" class="nav-link">
                    <i class="fas fa-chart-line"></i>
                    UWS-Monitoring
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-s3.html" class="nav-link">
                    <i class="fas fa-cloud"></i>
                    UWS-S3
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-compute.html" class="nav-link">
                    <i class="fas fa-server"></i>
                    UWS-Compute
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-lambda.html" class="nav-link">
                    <i class="fas fa-code"></i>
                    UWS-Lambda
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-rdb.html" class="nav-link">
                    <i class="fas fa-database"></i>
                    UWS-RDB
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-sqs.html" class="nav-link">
                    <i class="fas fa-envelope-open-text"></i>
                    UWS-SQS
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-nosql.html" class="nav-link">
                    <i class="fas fa-database"></i>
                    UWS-NoSQL
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-ai.html" class="nav-link">
                    <i class="fas fa-brain"></i>
                    UWS-AI
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-secrets.html" class="nav-link">
                    <i class="fas fa-key"></i>
                    UWS-Secrets
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-dns.html" class="nav-link">
                    <i class="fas fa-globe"></i>
                    UWS-DNS
                </a>
            </div>
            <div class="nav-item">
                <a href="/uws-iam.html" class="nav-link">
                    <i class="fas fa-users-cog"></i>
                    UWS-IAM
                </a>
            </div>
        </nav>
    </div>

    <!-- Main Content -->
    <div class="main-content">
        <!-- Top Navigation -->
        <div class="top-navbar">
            <div class="d-flex align-items-center">
                <button class="sidebar-toggle me-3" id="sidebarToggle">
                    <i class="fas fa-bars"></i>
                </button>
                <h4 class="mb-0">{page_title}</h4>
            </div>
            <button class="btn btn-outline-danger" onclick="logout()">Logout</button>
        </div>'''

# Sidebar CSS
SIDEBAR_CSS = '''        /* Sidebar Styles */
        .sidebar {
            position: fixed;
            top: 0;
            left: 0;
            height: 100vh;
            width: 280px;
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            box-shadow: 2px 0 20px rgba(0, 0, 0, 0.1);
            z-index: 1000;
            overflow-y: auto;
            transition: transform 0.3s ease;
        }
        
        .sidebar-header {
            padding: 1.5rem;
            border-bottom: 1px solid rgba(0, 0, 0, 0.1);
            text-align: center;
        }
        
        .sidebar-brand {
            font-size: 1.5rem;
            font-weight: bold;
            color: #667eea;
            text-decoration: none;
        }
        
        .sidebar-nav {
            padding: 1rem 0;
        }
        
        .nav-item {
            margin: 0.25rem 1rem;
        }
        
        .nav-link {
            display: flex;
            align-items: center;
            padding: 0.75rem 1rem;
            color: #495057;
            text-decoration: none;
            border-radius: 10px;
            transition: all 0.3s ease;
            font-weight: 500;
        }
        
        .nav-link:hover {
            background: linear-gradient(45deg, #667eea, #764ba2);
            color: white;
            transform: translateX(5px);
        }
        
        .nav-link.active {
            background: linear-gradient(45deg, #667eea, #764ba2);
            color: white;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
        }
        
        .nav-link i {
            width: 20px;
            margin-right: 12px;
            font-size: 1.1rem;
        }
        
        /* Main Content */
        .main-content {
            margin-left: 280px;
            min-height: 100vh;
            padding: 2rem;
        }
        
        .top-navbar {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
            border-radius: 15px;
            padding: 1rem 2rem;
            margin-bottom: 2rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .sidebar-toggle {
            display: none;
            background: none;
            border: none;
            font-size: 1.5rem;
            color: #667eea;
        }
        
        /* Responsive */
        @media (max-width: 768px) {
            .sidebar {
                transform: translateX(-100%);
            }
            
            .sidebar.show {
                transform: translateX(0);
            }
            
            .main-content {
                margin-left: 0;
                padding: 1rem;
            }
            
            .sidebar-toggle {
                display: block !important;
            }
        }'''

# Sidebar JavaScript
SIDEBAR_JS = '''        // Sidebar functionality
        document.addEventListener('DOMContentLoaded', function() {
            const sidebarToggle = document.getElementById('sidebarToggle');
            const sidebar = document.getElementById('sidebar');
            
            if (sidebarToggle) {
                sidebarToggle.addEventListener('click', function() {
                    sidebar.classList.toggle('show');
                });
            }
            
            // Close sidebar when clicking outside on mobile
            document.addEventListener('click', function(event) {
                if (window.innerWidth <= 768) {
                    if (!sidebar.contains(event.target) && !sidebarToggle.contains(event.target)) {
                        sidebar.classList.remove('show');
                    }
                }
            });
            
            // Set active nav link based on current page
            const currentPage = window.location.pathname;
            const navLinks = document.querySelectorAll('.nav-link');
            
            navLinks.forEach(link => {
                if (link.getAttribute('href') === currentPage) {
                    link.classList.add('active');
                } else {
                    link.classList.remove('active');
                }
            });
        });'''

def get_page_title(filename):
    """Get the page title based on filename"""
    titles = {
        'uws-billing.html': 'UWS-Billing & Cost Management',
        'uws-monitoring.html': 'UWS-Monitoring Dashboard',
        'uws-s3.html': 'UWS-S3 Cloud Storage',
        'uws-compute.html': 'UWS-Compute Container Service',
        'uws-lambda.html': 'UWS-Lambda Function Service',
        'uws-rdb.html': 'UWS-RDB Relational Database',
        'uws-sqs.html': 'UWS-SQS Message Queue',
        'uws-nosql.html': 'UWS-NoSQL Document Database',
        'uws-ai.html': 'UWS-AI Serverless AI',
        'uws-secrets.html': 'UWS-Secrets Manager',
        'uws-dns.html': 'UWS-DNS Domain Service',
        'uws-iam.html': 'UWS-IAM Identity Management'
    }
    return titles.get(filename, 'UWS Service')

def apply_sidebar_to_file(filepath):
    """Apply sidebar to a single HTML file"""
    print(f"Processing: {filepath}")
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    filename = os.path.basename(filepath)
    page_title = get_page_title(filename)
    
    # Skip if already has sidebar
    if 'class="sidebar"' in content:
        print(f"  Skipping {filename} - already has sidebar")
        return
    
    # Add Font Awesome if not present
    if 'font-awesome' not in content and 'fontawesome' not in content:
        content = content.replace(
            '<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">',
            '<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">\n    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">'
        )
    
    # Add overflow-x: hidden to body
    if 'overflow-x: hidden' not in content:
        content = re.sub(
            r'body\s*\{[^}]*\}',
            lambda m: m.group(0).replace('min-height: 100vh;', 'min-height: 100vh;\n            overflow-x: hidden;'),
            content
        )
    
    # Add sidebar CSS
    if '/* Sidebar Styles */' not in content:
        # Find the last style rule and add sidebar CSS before it
        style_end = content.rfind('</style>')
        if style_end != -1:
            content = content[:style_end] + SIDEBAR_CSS + '\n        ' + content[style_end:]
    
    # Replace body content with sidebar
    if '<body>' in content and 'class="sidebar"' not in content:
        # Find the main container or first div after body
        body_start = content.find('<body>')
        body_end = content.find('</body>')
        
        if body_start != -1 and body_end != -1:
            body_content = content[body_start + 6:body_end].strip()
            
            # Create new body content with sidebar
            new_body_content = SIDEBAR_HTML.format(page_title=page_title) + '\n\n        ' + body_content
            
            content = content[:body_start + 6] + '\n' + new_body_content + '\n    ' + content[body_end:]
    
    # Add sidebar JavaScript
    if '// Sidebar functionality' not in content:
        # Find the last script tag and add sidebar JS before it
        script_end = content.rfind('</script>')
        if script_end != -1:
            content = content[:script_end] + '\n\n        ' + SIDEBAR_JS + '\n    ' + content[script_end:]
    
    # Write back to file
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"  Applied sidebar to {filename}")

def main():
    """Main function to apply sidebar to all UWS pages"""
    static_dir = Path('src/main/resources/static')
    
    if not static_dir.exists():
        print("Error: static directory not found")
        return
    
    # List of UWS pages to process
    uws_pages = [
        'uws-billing.html',
        'uws-monitoring.html',
        'uws-s3.html',
        'uws-compute.html',
        'uws-lambda.html',
        'uws-rdb.html',
        'uws-sqs.html',
        'uws-nosql.html',
        'uws-ai.html',
        'uws-secrets.html',
        'uws-dns.html',
        'uws-iam.html'
    ]
    
    print("Applying modern sidebar to UWS pages...")
    print("=" * 50)
    
    for page in uws_pages:
        filepath = static_dir / page
        if filepath.exists():
            apply_sidebar_to_file(filepath)
        else:
            print(f"Warning: {page} not found")
    
    print("=" * 50)
    print("Sidebar application completed!")

if __name__ == "__main__":
    main() 