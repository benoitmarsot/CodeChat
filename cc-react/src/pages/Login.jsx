import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../services/auth";
import "./Login.css";

function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [token, setToken] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    const handleLogin = async () => {
        try {
            setError(""); // Clear any previous errors
            const data = await login(email, password);
            setToken(data.token);
            console.log("Login successful:", data);
            navigate('/projects'); // Redirect to projects page
        } catch (error) {
            setError( "Login failed. Please try again.");
            console.error("Login failed:", error);
        }
    };

    return (
        <div className="login-container">
            <h1>CodeChat</h1>
            <div className="login-form">
                <h2>Sign In</h2>
                {error && <div className="error-message">{error}</div>}
                <input 
                    type="email" 
                    placeholder="Email" 
                    value={email} 
                    onChange={(e) => setEmail(e.target.value)} 
                />
                <input 
                    type="password" 
                    placeholder="Password" 
                    value={password} 
                    onChange={(e) => setPassword(e.target.value)} 
                />
                <button onClick={handleLogin}>Login</button>
                {token && <p>Token: {token}</p>}
            </div>
        </div>
    );
}

export default Login;