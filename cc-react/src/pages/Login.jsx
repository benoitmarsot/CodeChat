import React, { useState } from "react";
import { login } from "../services/auth";
import "./Login.css";

function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [token, setToken] = useState("");

    const handleLogin = async () => {
        try {
            const data = await login(email, password);
            setToken(data.token);
            console.log("Login successful:", data);
        } catch (error) {
            console.error("Login failed:", error);
        }
    };

    return (
        <div className="login-container">
            <h1>CodeChat</h1>
            <div className="login-form">
                <h2>Sign In</h2>
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