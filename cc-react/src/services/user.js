import axios from "axios";

const API_URL = "http://localhost:8080/api/users/";

export const getUsers = async () => {
    const response = await axios.get(API_URL+"users");
    return response.data;
};

