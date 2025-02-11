import axios from "axios";

const API_URL = "http://localhost:8080/auth/";

export const register = async (user) => {
    const response = await axios.post(API_URL+"register", user);
    return response.data;
};

export const login = async (email, password) => {
    const response = await axios.post(API_URL + "login", { email, password });
    return response.data;
};