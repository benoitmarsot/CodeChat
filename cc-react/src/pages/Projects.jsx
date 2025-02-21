import React, { useState } from 'react';
import { ResizableBox } from 'react-resizable';
import SyntaxHighlighter from 'react-syntax-highlighter';
import { vs } from 'react-syntax-highlighter/dist/esm/styles/hljs';
import './Projects.css';

const Projects = () => {
    const [selectedTool, setSelectedTool] = useState('project');

    return (
        <div className="projects-layout">
            {/* Left Toolbar */}
            <div className="toolbar">
                <button 
                    className={`tool-btn ${selectedTool === 'project' ? 'active' : ''}`}
                    onClick={() => setSelectedTool('project')}
                >
                    <i className="fas fa-folder"></i>
                </button>
                <button 
                    className={`tool-btn ${selectedTool === 'files' ? 'active' : ''}`}
                    onClick={() => setSelectedTool('files')}
                >
                    <i className="fas fa-file-upload"></i>
                </button>
                <button 
                    className={`tool-btn ${selectedTool === 'discussions' ? 'active' : ''}`}
                    onClick={() => setSelectedTool('discussions')}
                >
                    <i className="fas fa-comments"></i>
                </button>
                <button 
                    className={`tool-btn ${selectedTool === 'messages' ? 'active' : ''}`}
                    onClick={() => setSelectedTool('messages')}
                >
                    <i className="fas fa-envelope"></i>
                </button>
            </div>

            {/* Tool Panel */}
            <div className="tool-panel">
                {selectedTool === 'project' && (
                    <div className="project-tools">
                        <button>New Project</button>
                        <button>Load Project</button>
                        <button>Delete Project</button>
                    </div>
                )}
                {/* Add other tool panels here */}
            </div>

            {/* Main Content Area */}
            <div className="main-content">
                {/* Code Viewer Panel */}
                <ResizableBox
                    width={Infinity}
                    height={300}
                    minConstraints={[Infinity, 100]}
                    maxConstraints={[Infinity, 800]}
                    axis="y"
                >
                    <div className="code-viewer">
                        <SyntaxHighlighter language="javascript" style={vs}>
                            {`// Sample code will be displayed here`}
                        </SyntaxHighlighter>
                    </div>
                </ResizableBox>

                {/* Bottom Split Panels */}
                <div className="bottom-panels">
                    <div className="context-panel">
                        <h3>Context</h3>
                        <div className="context-content">
                            {/* OpenAI context will be displayed here */}
                        </div>
                    </div>
                    <div className="chat-panel">
                        <div className="message-history">
                            {/* Messages will be displayed here */}
                        </div>
                        <div className="chat-input">
                            <textarea placeholder="Type your message..."></textarea>
                            <button>Send</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Projects;
