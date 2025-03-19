from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List

app = FastAPI(title="Simple Doodle API")


class Poll(BaseModel):
    title: str
    options: List[str]

class Vote(BaseModel):
    option: str


class DummyDatabase:
    def __init__(self):
        self.polls = {}
        self.poll_results = {}
        self.counter = 1

    def create_poll(self, poll: Poll):
        poll_id = self.counter
        self.polls[poll_id] = {"title": poll.title, "options": poll.options}
        self.poll_results[poll_id] = {option: 0 for option in poll.options}
        self.counter += 1
        return poll_id

    def list_polls(self):
        return self.polls

    def get_poll(self, poll_id: int):
        if poll_id in self.polls:
            return self.polls[poll_id]
        raise HTTPException(status_code=404, detail="Poll not found")

    def update_poll(self, poll_id: int, poll: Poll):
        if poll_id not in self.polls:
            raise HTTPException(status_code=404, detail="Poll not found")
        self.polls[poll_id] = {"title": poll.title, "options": poll.options}
        self.poll_results[poll_id] = {option: 0 for option in poll.options}
        return self.polls[poll_id]

    def delete_poll(self, poll_id: int):
        if poll_id not in self.polls:
            raise HTTPException(status_code=404, detail="Poll not found")
        del self.polls[poll_id]
        del self.poll_results[poll_id]
        return True

    def vote(self, poll_id: int, vote: Vote):
        if poll_id not in self.polls:
            raise HTTPException(status_code=404, detail="Poll not found")
        if vote.option not in self.poll_results[poll_id]:
            raise HTTPException(status_code=400, detail="Option not found")
        self.poll_results[poll_id][vote.option] += 1

    def get_results(self, poll_id: int):
        if poll_id not in self.polls:
            raise HTTPException(status_code=404, detail="Poll not found")
        return self.poll_results[poll_id]


db = DummyDatabase()

@app.get("/polls")
def list_polls():
    return db.list_polls()

@app.post("/polls", status_code=201)
def create_poll(poll: Poll):
    poll_id = db.create_poll(poll)
    return {"poll_id": poll_id, "poll": db.get_poll(poll_id)}

@app.get("/polls/{poll_id}")
def get_poll(poll_id: int):
    return db.get_poll(poll_id)

@app.put("/polls/{poll_id}")
def update_poll(poll_id: int, poll: Poll):
    return db.update_poll(poll_id, poll)

@app.delete("/polls/{poll_id}")
def delete_poll(poll_id: int):
    db.delete_poll(poll_id)
    return {"detail": "Poll has been deleted."}

@app.post("/polls/{poll_id}/vote")
def vote(poll_id: int, vote: Vote):
    db.vote(poll_id, vote)
    return {"detail": "Vote sent!"}

@app.get("/polls/{poll_id}/vote")
def get_results(poll_id: int):
    return db.get_results(poll_id)
