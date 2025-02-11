import { useEffect, useState } from "react";
import { getUsers} from "../services/user";
import { register} from "../services/auth";

function UserList() {
    const [users, setUsers] = useState([]);
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [role, setRole] = useState("");

    useEffect(() => {
        fetchUsers();
    }, []);

    const fetchUsers = async () => {
        const data = await getUsers();
        setUsers(data);
    };

    const handleAddUser = async () => {
        if (name && email && password && role) {
            await register({ name, email, password, role });
            fetchUsers();
            setName("");
            setEmail("");
            setPassword("");
            setRole("");
        }
    };

    return (
        <div>
            <h1>User List</h1>
            <input 
                type="text" 
                placeholder="Name" 
                value={name} 
                onChange={(e) => setName(e.target.value)} 
            />
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
            <input 
                type="text" 
                placeholder="Role" 
                value={role} 
                onChange={(e) => setRole(e.target.value)} 
            />
            <button onClick={handleAddUser}>Add User</button>
            <ul>
                {users.map((user) => (
                    <li key={user.id}>{user.name} - {user.email}</li>
                ))}
            </ul>
        </div>
    );
}

export default UserList;