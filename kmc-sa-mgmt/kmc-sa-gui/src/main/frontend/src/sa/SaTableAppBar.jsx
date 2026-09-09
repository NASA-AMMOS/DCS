/*
 * Copyright 2022, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology
 * Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. By accepting
 * this software, the user agrees to comply with all applicable U.S.
 * export laws and regulations. User has the responsibility to obtain
 * export licenses, or other export authority as may be required before
 * exporting such information to foreign countries or providing access to
 * foreign persons.
 */
/**
 * @author panjames
 */
import {
    AppBar,
    Box,
    Button,
    Checkbox,
    Container,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControlLabel,
    FormGroup,
    Input,
    LinearProgress,
    List,
    ListItem,
    Popover,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Toolbar,
    Typography
} from "@mui/material";
import {Add, DeleteForever, FileDownload, Queue, Refresh} from "@mui/icons-material";
import React, {useState} from "react";
import {bulkCreate, createErrorCallback, createResponseCallback, deleteSas, downloadCsv} from "./api";

export default function SaTableAppBar({
                                          resetDefaultCols,
                                          allColumns,
                                          hideColumn,
                                          selectedFlatRows,
                                          refreshTable,
                                          enqueueSnackbar,
                                          handleRowOpen,
                                          type
                                      }) {

    /**
     * Popovers
     */
    const [anchorEl, setAnchorEl] = useState(null)
    const [anchorElId, setAnchorElId] = useState(null)

    /**
     * Handle popover open clicks
     * @param e event
     * @param id popover id
     */
    const handlePopoverClick = (e, id) => {
        setAnchorEl(e.target)
        setAnchorElId(id)
    }

    /**
     * Handle popover close
     */
    const handlePopoverClose = () => {
        setAnchorEl(null)
        setAnchorElId(null)
    }

    /**
     * Utility row stuff
     */
    const [deleteOpen, setDeleteOpen] = useState(false)
    const [bulkOpen, setBulkOpen] = useState(false)
    const [selectedFile, setSelectedFile] = useState(null)
    const [replaceExisting, setReplaceExisting] = useState(false)

    /**
     * Bulk delete from table selection
     */
    const bulkDelete = () => {
        let ids = selectedFlatRows.map(d => d.original.id)
        deleteSas(type, ids,
            createResponseCallback(`Deleted ${ids.length} SAs`, setDeleteOpen, refreshTable, enqueueSnackbar),
            createErrorCallback(enqueueSnackbar))
    }

    /**
     * Upload CSV file onChange function
     * @param event onChange event
     */
    const uploadFileChange = (event) => {
        if (event.target.files.length > 0) {
            setSelectedFile(event.target.files[0])
        } else {
            setSelectedFile(null)
        }
    }

    /**
     * Upload CSV progress
     */
    const [progress, setProgress] = useState(0)

    /**
     * Upload response callback
     * @param r axios response
     */
    const uploadRespCallback = (r) => {
        const responseCallback = createResponseCallback('Uploaded CSV',
            setBulkOpen,
            refreshTable,
            enqueueSnackbar)
        responseCallback(r)
        setProgress(0)
        setSelectedFile(null)
    }

    /**
     * Upload error callback
     * @param e axios error
     */
    const errCallback = (e) => {
        const errorCallback = createErrorCallback(enqueueSnackbar)
        errorCallback(e)
        setProgress(0)
        refreshTable()
    }

    const [tmChecked, setTmChecked] = useState(true)
    const [tcChecked, setTcChecked] = useState(true)
    const [aosChecked, setAosChecked] = useState(true)

    /**
     * Upload CSV file
     */
    const uploadCsv = () => {
        const frameTypes = []
        if (tmChecked) {
            frameTypes.push('TM')
        }
        if (tcChecked) {
            frameTypes.push('TC')
        }
        if (aosChecked) {
            frameTypes.push('AOS')
        }
        const formData = new FormData()
        formData.append("file",
            selectedFile,
            selectedFile.name)
        formData.append("force",
            replaceExisting)
        bulkCreate(frameTypes,
            formData,
            setProgress,
            uploadRespCallback,
            errCallback)
    }

    /**
     * Download CSV file
     */
    const download = () => {
        downloadCsv((r) => {
            const url = window.URL.createObjectURL(new Blob([r.data]))
            const link = document.createElement('a')
            link.href = url
            link.setAttribute('download', r.headers['x-suggested-filename'])
            link.click()
            enqueueSnackbar('Downloaded CSV', {variant: "success"})
        }, () => {
            enqueueSnackbar(`Error downloading CSV`, {variant: "error"})
        })
    }

    const handleFrameTypeCheckbox = (e) => {
        console.log(e.target)
        const {value, checked} = e.target
        console.log(`value: ${value}, checked: ${checked}`)
        switch (value) {
            case "TM": {
                console.log(`changing tm to ${checked}`)
                setTmChecked(checked)
                break
            }
            case "AOS": {
                console.log(`changing aos to ${checked}`)
                setAosChecked(checked)
                break
            }
            case "TC": {
                console.log(`changing tc to ${checked}`)
                setTcChecked(checked)
                break
            }
        }
        console.log(`tc: ${tcChecked}`)
        console.log(`tm: ${tmChecked}`)
        console.log(`aos: ${aosChecked}`)
    }

    return (
        <AppBar position={"static"}>
            <Toolbar>
                <Box sx={{flexGrow: 1}}>
                    <Button variant={"contained"} edge={"begin"}
                            onClick={(e) => handlePopoverClick(e, "colSel")}>Columns</Button>
                    <Popover open={anchorElId === "colSel"}
                             id={"colSel"}
                             anchorEl={anchorEl}
                             onClose={handlePopoverClose}
                             anchorOrigin={{
                                 vertical: 'bottom',
                                 horizontal: 'left'
                             }}
                    >
                        <Box sx={{padding: "16px"}}>
                            <Box sx={{marginBottom: "5px"}} key={"default-cols"}>
                                <Button variant={"contained"} onClick={resetDefaultCols}
                                        startIcon={<Refresh/>}>Reset</Button>
                            </Box>
                            {allColumns.map(col => col.id === 'selection' ? ('') : (
                                <div key={col.id}>
                                    <label>
                                        <input type={"checkbox"}
                                               onClick={(e) => {
                                                   hideColumn(col.id, e.target.checked)
                                               }}
                                               {...col.getToggleHiddenProps()} />{' '}{col.Header}
                                    </label>
                                </div>
                            ))}
                        </Box>
                    </Popover>
                </Box>
                <Stack direction={"row"} spacing={1}>
                    <Button variant={"contained"} color={"error"} edge={"end"} onClick={() => setDeleteOpen(true)}
                            startIcon={<DeleteForever/>} disabled={selectedFlatRows.length === 0}>DELETE</Button>
                    <Dialog open={deleteOpen} onClose={() => setDeleteOpen(false)}>
                        <DialogTitle>Bulk Delete SAs</DialogTitle>
                        <DialogContent>
                            <Container>
                                This will permanently delete the following SAs (SPI/SCID):
                                <List>
                                    {selectedFlatRows.map(d => {
                                        return (
                                            <ListItem
                                                key={d.original.spi + '-' + d.original.scid}>&bull; {d.original.spi}/{d.original.scid}</ListItem>
                                        )
                                    })}
                                </List>
                            </Container>
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={bulkDelete}
                                    variant={"contained"}
                                    color={"error"}>
                                Continue
                            </Button>
                            <Button onClick={() => setDeleteOpen(false)}
                                    variant={"contained"}>
                                Cancel
                            </Button>
                        </DialogActions>
                    </Dialog>
                    <Button edge={"end"} onClick={() => handleRowOpen(undefined, false)} startIcon={<Add/>}
                            variant={"contained"}>Create</Button>
                    <Button variant={"contained"}
                            edge={"end"}
                            startIcon={<Queue/>}
                            onClick={() => setBulkOpen(true)}>
                        Bulk</Button>
                    <Dialog open={bulkOpen}
                            onClose={() => setBulkOpen(false)}>
                        <DialogTitle>Upload CSV</DialogTitle>
                        <DialogContent>Bulk creation of SAs by uploading a CSV file
                            <Table>
                                <TableHead>
                                    <TableRow>
                                        <TableCell>File Name</TableCell>
                                        <TableCell>File Size (B)</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    <TableRow>
                                        <TableCell>{selectedFile?.name}</TableCell>
                                        <TableCell>{selectedFile?.size}</TableCell>
                                    </TableRow>
                                </TableBody>
                            </Table>
                            <Box sx={{display: 'flex', alignItems: 'center'}}>
                                <Box sx={{width: '100%', mr: 1}}>
                                    <LinearProgress variant={"determinate"} value={progress}/>
                                </Box>
                                <Box sx={{minWidth: 35}}>
                                    <Typography variant={"body2"}
                                                color={"text.secondary"}>{`${Math.round(progress)}`}%</Typography>
                                </Box>
                            </Box>
                        </DialogContent>
                        <DialogActions>
                            <Stack>
                                <Container>
                                    <label htmlFor={"contained-button-file"}>
                                        <Input accept={".csv"} id={"contained-button-file"}
                                               type={"file"}
                                               onChange={uploadFileChange}/>
                                        <Button variant={"contained"}
                                                onClick={() => uploadCsv()}
                                                disabled={selectedFile == null}
                                        >Continue</Button>
                                    </label>
                                </Container>
                                <Container>
                                    <FormControlLabel control={<Checkbox checked={replaceExisting}
                                                                         onChange={(event, checked) => setReplaceExisting(checked)}/>}
                                                      label={"Replace existing"}></FormControlLabel>
                                </Container>
                                <Container>
                                    <Typography variant={'h6'} sx={{textAlign: 'center'}}>Frame Types</Typography>
                                    <FormGroup sx={{position: 'flex', flexDirection: 'row', justifyContent: 'center'}}>
                                        <FormControlLabel control={<Checkbox checked={tcChecked}/>} value={"TC"} label={"TC"}
                                                          onChange={handleFrameTypeCheckbox}/>
                                        <FormControlLabel control={<Checkbox checked={tmChecked}/>} value={"TM"} label={"TM"}
                                                          onChange={handleFrameTypeCheckbox}/>
                                        <FormControlLabel control={<Checkbox checked={aosChecked}/>} value={"AOS"}
                                                          label={"AOS"}
                                                          onChange={handleFrameTypeCheckbox}/>
                                    </FormGroup>
                                </Container>
                            </Stack>
                        </DialogActions>
                    </Dialog>
                    <Button variant={"contained"} edge={"end"} startIcon={<FileDownload/>}
                            onClick={download}>Export</Button>
                    <Button variant={"contained"} edge={"end"} startIcon={<Refresh/>}
                            onClick={refreshTable}>Reload</Button>
                </Stack>
            </Toolbar>
        </AppBar>
    );
}