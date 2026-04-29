from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
import joblib
import os

app = FastAPI(title="Task Assignment AI Service")

MODEL_PATH = "task_model.joblib"

# Load model if exists, else create a dummy model for demo
if os.path.exists(MODEL_PATH):
    model = joblib.load(MODEL_PATH)
else:
    model = RandomForestRegressor(n_estimators=100, random_state=42)
    # Dummy training so it can predict
    dummy_X = pd.DataFrame({"difficulty": [1, 5, 10], "estimated_time": [2.0, 10.0, 20.0]})
    dummy_y = pd.Series([2.5, 9.5, 22.0])
    model.fit(dummy_X, dummy_y)

class TaskInput(BaseModel):
    difficulty: int
    category: str
    estimated_time: float
    deadline_days: int

class UserInput(BaseModel):
    id: int
    avg_time: float
    workload: float # 0.0 to 1.0 (load factor)
    reliability: float # 0.0 to 1.0

class PredictRequest(BaseModel):
    task: TaskInput
    users: List[UserInput]

class Recommendation(BaseModel):
    user_id: int
    score: float
    predicted_time: float
    reason: str

class PredictResponse(BaseModel):
    recommendations: List[Recommendation]

class TrainTask(BaseModel):
    difficulty: int
    estimated_time: float
    actual_time: float

class TrainRequest(BaseModel):
    tasks: List[TrainTask]

@app.get("/health")
def health_check():
    return {"status": "OK"}

@app.post("/train")
def train_model(req: TrainRequest):
    if not req.tasks:
        raise HTTPException(status_code=400, detail="No training data provided")
    
    df = pd.DataFrame([t.dict() for t in req.tasks])
    X = df[["difficulty", "estimated_time"]]
    y = df["actual_time"]
    
    global model
    model = RandomForestRegressor(n_estimators=100, random_state=42)
    model.fit(X, y)
    joblib.dump(model, MODEL_PATH)
    
    return {"status": "success", "message": "Model trained successfully"}

@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    if not req.users:
        raise HTTPException(status_code=400, detail="No users provided")
    
    task_df = pd.DataFrame([{
        "difficulty": req.task.difficulty,
        "estimated_time": req.task.estimated_time
    }])
    
    # Predict base completion time for this task (simplified model ignoring user specifics for base prediction)
    base_predicted_time = model.predict(task_df)[0]
    
    recommendations = []
    
    # Alpha, Beta, Gamma weights
    alpha = 10.0
    beta = 5.0
    gamma = 5.0
    
    for user in req.users:
        # Adjust predicted time based on user's average time factor
        # For a real system, you'd feed user stats into the model. Here we approximate.
        user_predicted_time = base_predicted_time * (user.avg_time / req.task.estimated_time if req.task.estimated_time > 0 else 1)
        
        # Calculate Score
        # Score = α * (1 / predicted_time) + β * availability + γ * reliability
        # availability is (1 - workload)
        availability = 1.0 - user.workload
        
        score = alpha * (1 / user_predicted_time if user_predicted_time > 0 else 1) + \
                beta * availability + \
                gamma * user.reliability
                
        # Apply fairness: AdjustedScore = Score * (1 - load_factor)
        adjusted_score = score * (1.0 - user.workload)
        
        # Determine reason
        if user.workload > 0.8:
            reason = "High workload, but highly reliable."
        elif user.reliability > 0.8 and user.workload < 0.5:
            reason = "Optimal choice: High reliability, low workload."
        else:
            reason = "Balanced metrics."
            
        recommendations.append(Recommendation(
            user_id=user.id,
            score=round(adjusted_score, 2),
            predicted_time=round(user_predicted_time, 2),
            reason=reason
        ))
        
    # Sort by score descending and take top 3
    recommendations.sort(key=lambda x: x.score, reverse=True)
    top_3 = recommendations[:3]
    
    return PredictResponse(recommendations=top_3)

# --- N8N AUTOMATION MIDDLEWARE ---

# In memory state for toggles
automation_status_db = {
    "auto_assign": False,
    "auto_rebalance": False,
    "auto_cleanup": False,
    "auto_retrain": False
}

class ToggleRequest(BaseModel):
    status: bool

@app.post("/trigger-auto-assign")
def trigger_auto_assign():
    # Here you would use requests.post() to call your actual n8n webhook
    # requests.post("http://n8n-url/webhook/auto-assign")
    return {"status": "success", "message": "n8n workflow triggered: Auto Assign"}

@app.post("/trigger-rebalance")
def trigger_rebalance():
    return {"status": "success", "message": "n8n workflow triggered: Rebalance Workload"}

@app.post("/trigger-cleanup")
def trigger_cleanup_users():
    return {"status": "success", "message": "n8n workflow triggered: Clean Inactive Users"}

@app.post("/trigger-optimize")
def trigger_optimize_deadlines():
    return {"status": "success", "message": "n8n workflow triggered: Optimize Deadlines"}

@app.get("/automation-status")
def get_automation_status():
    return automation_status_db

@app.post("/automation-status/{toggle}")
def update_automation_status(toggle: str, req: ToggleRequest):
    if toggle in automation_status_db:
        automation_status_db[toggle] = req.status
        return {"status": "success", "state": automation_status_db}
    raise HTTPException(status_code=404, detail="Toggle not found")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
