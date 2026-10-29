/*
 * Copyright (c) 2014-26 California Institute of Technology (“Caltech”). U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *  • Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *  • Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *  • Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Open Source License Approved by Caltech/JPL
 * APACHE LICENSE, VERSION 2.0
 *  • Text version: https://www.apache.org/licenses/LICENSE-2.0.txt
 *  • SPDX short identifier: Apache-2.0
 *  • OSI Approved License: https://opensource.org/licenses/Apache-2.0
 */
/**
 * @author panjames
 */
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    InputAdornment,
    Stack,
    Typography
} from "@mui/material";
import {Close, DeleteForever, Key, PlayArrow, Restore, Stop} from "@mui/icons-material";
import {useState} from "react";
import {useFormik} from "formik";
import {hexString, SaTextField} from "./SaModalForm.jsx";
import * as yup from "yup";
import {useSnackbar} from "notistack";
import {modifyArsn, modifyIv, rekeySa} from "./api";
import {AUTHENTICATED_ENCRYPTION, AUTHENTICATION, ENCRYPTION} from "./utilities";

export default function EditControlStack({
                                             refresh,
                                             setModalOpen,
                                             defaultValues,
                                             data,
                                             id,
                                             saState,
                                             deleteAction,
                                             stopAction,
                                             startAction,
                                             expireAction,
                                         }) {
    const [deleteOpen, setDeleteOpen] = useState(false)
    const [startOpen, setStartOpen] = useState(false)
    const [stopOpen, setStopOpen] = useState(false)
    const [expireOpen, setExpireOpen] = useState(false)
    const [resetOpen, setResetOpen] = useState(false)
    const [rekeyOpen, setRekeyOpen] = useState(false)

    const {enqueueSnackbar} = useSnackbar()

    const validationSchema = yup.object({
        ivLen: yup.number().min(0),
        iv: hexString,
        arsnLen: yup.number().min(0),
        arsn: hexString,
        arsnw: yup.number().min(0),
    })

    const rekeyValidationSchema = yup.object({
        ekid: yup.string().nullable(),
        akid: yup.string().nullable(),
    })

    const resetFormik = useFormik(
        {
            initialValues: data === undefined ? {
                id: defaultValues.id,
                iv: defaultValues.iv,
                ivLen: defaultValues.ivLen,
                arsn: defaultValues.arsn,
                arsnLen: defaultValues.arsnLen,
                arsnw: defaultValues.arsnw
            } : {
                id: data.id,
                iv: data.iv,
                ivLen: data.ivLen,
                arsn: data.arsn,
                arsnLen: data.arsnLen,
                arsnw: data.arsnw
            },
            validationSchema: validationSchema,
            onSubmit: (values) => {
                console.log('submitting')
                const clone = structuredClone(values)
                console.log(clone)
                if (values.iv !== resetFormik.initialValues.iv ||
                    values.ivLen !== resetFormik.initialValues.ivLen) {
                    console.log('updating iv')
                    modifyIv(clone,
                        (r) => {
                            setResetOpen(false)
                            refresh()
                            console.log(r)
                            enqueueSnackbar(`Updated IV on ${values.id.spi}/${values.id.scid}`, {variant: 'success'})
                        }, (e) => {
                            console.log(e)
                            enqueueSnackbar(`${e.response.status}: ${e.message}`, {variant: 'error'})
                        })
                } else {
                    console.log("no change to iv")
                }
                if (values.arsn !== resetFormik.initialValues.arsn ||
                    values.arsnLen !== resetFormik.initialValues.arsnLen ||
                    values.arsnw !== resetFormik.initialValues.arsnw) {
                    console.log('updating arsn')
                    modifyArsn(clone, (r) => {
                        setResetOpen(false)
                        refresh()
                        console.log(r)
                        enqueueSnackbar(`Updated ARSN on ${values.id.spi}/${values.id.scid}`, {variant: 'success'})
                    }, (e) => {
                        console.log(e)
                        enqueueSnackbar(`${e.response.status}: ${e.message}`, {variant: 'error'})
                    })
                } else {
                    console.log("no change to arsn")
                }
            },
            enableReinitialize: true
        },
    )

    const rekeyFormik = useFormik({
        initialValues: data === undefined ? {
            ekid: defaultValues.ekid,
            akid: defaultValues.akid,
        } : {
            ekid: data.ekid,
            akid: data.akid,
        },
        validationSchema: rekeyValidationSchema,
        onSubmit: (values) => {
            let clone = structuredClone(values)
            const rekey = {
                id: data.id
            }
            if (clone.ekid !== rekeyFormik.initialValues.ekid ||
                clone.ecs !== rekeyFormik.initialValues.ecs ||
                clone.ecsLen !== rekeyFormik.initialValues.ecsLen) {
                console.log("updating ekid")
                rekey.ekid = clone.ekid
                rekey.ecs = clone.ecs
                rekey.ecsLen = clone.ecsLen
            }
            if (clone.akid !== rekeyFormik.initialValues.akid ||
                clone.acs !== rekeyFormik.initialValues.acs ||
                clone.acsLen !== rekeyFormik.initialValues.acsLen) {
                console.log("updating akid")
                rekey.akid = clone.akid
                rekey.acs = clone.acs
                rekey.acsLen = clone.acsLen
            }
            rekeySa(rekey,
                (r) => {
                    setRekeyOpen(false)
                    refresh()
                    console.log(r)
                    enqueueSnackbar(`Updated keys on ${values.id.spi}/${values.id.scid}`, {variant: 'success'})
                }, (e) => {
                    console.log(e)
                    enqueueSnackbar(`${e.response.status}: ${e.message}`, {variant: 'error'})
                })
        },
        enableReinitialize: true
    })

    return (
        <Stack direction={"row"} spacing={1}>
            <Button variant={"contained"} color={"error"} startIcon={<DeleteForever/>}
                    onClick={() => setDeleteOpen(true)}>Delete</Button>
            <Dialog open={deleteOpen} onClose={() => setDeleteOpen(false)}
                    aria-labelledby={"confirm-delete"} aria-describedby={"confirm-delete"}>
                <DialogTitle>Confirm Deletion</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        This action will permanently delete
                        SA {id["spi"]}/{id["scid"]}. Are you sure you
                        would like to continue?
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button variant={"contained"} color={"error"}
                            onClick={() => {
                                deleteAction(id)
                                setModalOpen(false)
                            }}>Continue</Button>
                    <Button variant={"contained"}
                            onClick={() => setDeleteOpen(false)}>Cancel</Button>
                </DialogActions>
            </Dialog>
            <Button variant={"contained"} color={"warning"}
                    startIcon={<Stop/>}
                    onClick={() => setStopOpen(true)}
                    sx={{display: saState <= 2 ? 'none' : 'flex'}}>Stop</Button>
            <Dialog open={stopOpen} onClose={() => setStopOpen(false)}
                    aria-labelledby={"confirm-stop"}
                    aria-describedby={"confirm-stop"}>
                <DialogTitle>Stop SA</DialogTitle>
                <DialogContent>This action will stop
                    SA {id["spi"]}/{id["scid"]}, returning it
                    to
                    the KEYED state. Do you wish to continue?</DialogContent>
                <DialogActions>
                    <Button variant={"contained"} color={"warning"}
                            onClick={() => {
                                stopAction(id)
                                setStopOpen(false)
                            }}>Continue</Button>
                    <Button variant={"contained"}
                            onClick={() => setStopOpen(false)}>Cancel</Button>
                </DialogActions>
            </Dialog>
            <Button variant={"contained"}
                    color={"warning"}
                    startIcon={<PlayArrow/>}
                    onClick={() => {
                        if (data.serviceType === AUTHENTICATION) {
                            if (data.akid === null || data.akid === '' || data.acs === '00' || data.acs === '0x00' || data.acs === 0) {
                                enqueueSnackbar(`Unable to start ${data.spi}/${data.scid}, service type is ${data.serviceType} but AKID or ACS are not set.`)
                                return
                            }
                        }
                        if (data.serviceType === ENCRYPTION || data.serviceType === AUTHENTICATED_ENCRYPTION) {
                            if (data.ekid === null || data.ekid === '' || data.ecs === '00' || data.ecs === '0x00' || data.ecs === 0) {
                                enqueueSnackbar(`Unable to start ${data.spi}/${data.scid}, service type is ${data.serviceType} but EKID or ECS are not set.`)
                            }
                        }
                        setStartOpen(true);
                    }}
                    sx={{display: saState === 3 ? 'none' : 'flex'}}>Start</Button>
            <Dialog open={startOpen}
                    onClose={() => setStartOpen(false)}
                    aria-labelledby={"confirm-start"}
                    aria-describedby={"confirm-start"}>
                <DialogTitle>Start SA</DialogTitle>
                <DialogContent>This action will start
                    SA {id["spi"]}/{id["scid"]}, setting it to
                    OPERATIONAL state. This will replace any currently operational
                    SA on
                    this GVCID. Do you wish to continue?</DialogContent>
                <DialogActions>
                    <Button variant={"contained"}
                            color={"warning"}
                            onClick={() => {
                                startAction(id)
                                setStartOpen(false)
                            }}>
                        Continue
                    </Button>
                    <Button variant={"contained"}
                            onClick={() => setStartOpen(false)}>
                        Cancel
                    </Button>
                </DialogActions>
            </Dialog>
            <Button variant={"contained"}
                    color={"warning"}
                    startIcon={<Close/>}
                    onClick={() => setExpireOpen(true)}
                    sx={{display: saState === 1 ? 'none' : 'flex'}}>
                Expire
            </Button>
            <Dialog open={expireOpen}
                    onClose={() => setExpireOpen(false)}
                    aria-labelledby={"confirm-expire"}
                    aria-describedby={"confirm-expire"}>
                <DialogTitle>Expire SA</DialogTitle>
                <DialogContent>This action will expire the SA, setting it to the "Unkeyed | Expired" state. Do you wish
                    to continue?</DialogContent>
                <DialogActions>
                    <Button variant={"contained"}
                            color={"warning"}
                            onClick={() => {
                                expireAction(id)
                                setExpireOpen(false)
                            }}>
                        Continue
                    </Button>
                    <Button variant={"contained"}
                            onClick={() => setExpireOpen(false)}>
                        Cancel
                    </Button>
                </DialogActions>
            </Dialog>
            <Button variant={"contained"}
                    startIcon={<Restore/>}
                    onClick={() => setResetOpen(true)}>
                IV/ARSN
            </Button>
            <Dialog open={resetOpen}
                    onClose={() => setResetOpen(false)}
                    aria-labelledby={"confirm-arsn-reset"}
                    aria-describedby={"confirm-arsn-reset"}>
                <DialogTitle>Reset IV and/or ARSN</DialogTitle>
                <DialogContent>
                    <Typography>Reset IV or ARSN to 0, or a specific value.</Typography>
                    <form onSubmit={resetFormik.handleSubmit}>
                        <SaTextField
                            formik={resetFormik}
                            id={"iv"}
                            name={"iv"}
                            label={"IV"}
                            helperText={"Initialization Vector"}
                            InputProps={{
                                startAdornment: <InputAdornment position="start"
                                                                sx={{style: {fontFamily: 'Roboto Mono'}}}><Typography
                                    color={'warning.main'} fontWeight={'bold'}
                                    fontFamily={'Roboto Mono'}>0x</Typography></InputAdornment>
                            }}
                        />
                        <SaTextField
                            formik={resetFormik}
                            id={"ivLen"}
                            name={"ivLen"}
                            label={"IV Length"}
                            helperText={"IV Length in bytes"}
                        />
                        <SaTextField
                            formik={resetFormik}
                            id={"arsn"}
                            name={"arsn"}
                            label={"ARSN"}
                            helperText={"Anti-Replay Sequence Number"}
                            InputProps={{
                                startAdornment: <InputAdornment position="start"
                                                                sx={{style: {fontFamily: 'Roboto Mono'}}}><Typography
                                    color={'warning.main'} fontWeight={'bold'}
                                    fontFamily={'Roboto Mono'}>0x</Typography></InputAdornment>
                            }}
                        />
                        <SaTextField
                            formik={resetFormik}
                            id={"arsnLen"}
                            name={"arsnLen"}
                            label={"ARSN Length"}
                            helperText={"ARSN length in bytes"}
                        />
                        <SaTextField
                            formik={resetFormik}
                            id={"arsnw"}
                            name={"arsnw"}
                            label={"ARSNW"}
                            helperText={"ARSN Window"}
                        />
                    </form>
                </DialogContent>
                <DialogActions>
                    <Button variant={"contained"} type={"submit"} color={"warning"}
                            onClick={resetFormik.handleSubmit} disabled={!resetFormik.dirty}>Continue</Button>
                    <Button variant={"contained"}
                            onClick={() => setResetOpen(false)}>Cancel</Button>
                </DialogActions>
            </Dialog>
            <Button variant={"contained"}
                    startIcon={<Key/>}
                    onClick={() => setRekeyOpen(true)}>(Re)key</Button>
            <Dialog open={rekeyOpen}
                    onClose={() => setRekeyOpen(false)}
                    aria-labelledby={"confirm-rekey"}
                    aria-describedby={"confirm-rekey"}>
                <DialogTitle>(Re)key SA</DialogTitle>
                <DialogContent>Rekey Form Here
                    <form onSubmit={rekeyFormik.handleSubmit}>
                        <SaTextField
                            formik={rekeyFormik}
                            id={"ekid"}
                            name={"ekid"}
                            label={"EKID"}
                            helperText={"Encryption Key ID"}
                        />
                        <SaTextField
                            formik={rekeyFormik}
                            id={"akid"}
                            name={"akid"}
                            label={"AKID"}
                            helperText={"Authentication Key ID"}
                        />
                    </form>
                </DialogContent>
                <DialogActions>
                    <Button variant={"contained"}
                            onClick={rekeyFormik.handleSubmit}>Continue</Button>
                    <Button variant={"contained"}
                            onClick={() => setRekeyOpen(false)}>Cancel</Button>
                </DialogActions>
            </Dialog>
        </Stack>
    );
}
